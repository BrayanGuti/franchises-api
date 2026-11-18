package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import org.springframework.stereotype.Component;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.DynamoConditions;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch.BranchConditions;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.enhanced.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactUpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Renombra un producto moviendo su reserva de nombre, en una sola transacción:
 * <ol start="0">
 * <li>la sucursal existe y está activa;</li>
 * <li>se borra la reserva del nombre viejo (solo si es de este producto);</li>
 * <li>se crea la reserva del nombre nuevo (si otro la tiene: 409);</li>
 * <li>{@code SET name} en el producto, si su nombre sigue siendo el leído.</li>
 * </ol>
 * Si el nombre nuevo solo cambia mayúsculas, tildes o espacios, la reserva es la misma y solo se
 * actualiza el nombre. Si el producto cambió entre la lectura y la escritura se reintenta.
 */
@Component
class RenameProductOperation {

	static final int MAX_ATTEMPTS = 3;
	private static final int BRANCH_CHECK = 0;
	private static final int NEW_RESERVATION = 2;

	private final ProductTables tables;

	RenameProductOperation(ProductTables tables) {
		this.tables = tables;
	}

	Mono<Product> execute(String franchiseId, String branchId, String productId, String newName) {
		return Mono.defer(() -> read(franchiseId, branchId, productId)
				.switchIfEmpty(Mono.error(() -> new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, productId)))
				.flatMap(current -> rename(ProductItemMapper.toDomain(current), newName)))
				.retryWhen(Retry.max(MAX_ATTEMPTS - 1L)
						.filter(ProductChangedException.class::isInstance)
						.onRetryExhaustedThrow((spec, signal) ->
								new BusinessException(FranchiseError.PRODUCT_MODIFIED_CONCURRENTLY, productId)))
				.then(Mono.defer(() -> read(franchiseId, branchId, productId)))
				.map(ProductItemMapper::toDomain)
				.switchIfEmpty(Mono.error(() -> new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, productId)));
	}

	private Mono<ProductItem> read(String franchiseId, String branchId, String productId) {
		GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
				.key(DynamoKeys.productKey(franchiseId, branchId, productId))
				.consistentRead(true)
				.build();
		return Mono.fromFuture(() -> tables.products().getItem(request));
	}

	private Mono<Void> rename(Product current, String newName) {
		Product renamed = new Product(current.id(), current.franchiseId(), current.branchId(), newName, current.stock());
		boolean reservationMoves = !DynamoKeys.productNameSk(current.branchId(), current.name())
				.equals(DynamoKeys.productNameSk(current.branchId(), newName));

		TransactWriteItemsEnhancedRequest.Builder transaction = TransactWriteItemsEnhancedRequest.builder()
				.addConditionCheck(tables.products(), ConditionCheck.builder()
						.key(DynamoKeys.branchKey(current.franchiseId(), current.branchId()))
						.conditionExpression(BranchConditions.existsAndActive())
						.build());
		if (reservationMoves) {
			transaction
					.addDeleteItem(tables.productNames(), TransactDeleteItemEnhancedRequest.builder()
							.key(DynamoKeys.productNameKey(current.franchiseId(), current.branchId(), current.name()))
							.conditionExpression(ProductConditions.reservationMissingOrOwnedBy(current.id()))
							.build())
					.addPutItem(tables.productNames(), TransactPutItemEnhancedRequest.builder(ProductNameItem.class)
							.item(ProductItemMapper.toNameItem(renamed))
							.conditionExpression(DynamoConditions.ITEM_NOT_EXISTS)
							.build());
		}
		transaction.addUpdateItem(tables.products(), TransactUpdateItemEnhancedRequest.builder(ProductItem.class)
				.item(onlyName(current, newName))
				.ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
				.conditionExpression(ProductConditions.existsWithName(current.name()))
				.build());

		return Mono.fromFuture(() -> tables.client().transactWriteItems(transaction.build()))
				.onErrorMap(TransactionCanceledException.class, e -> translate(e, current, newName, reservationMoves));
	}

	private static ProductItem onlyName(Product product, String newName) {
		ProductItem item = new ProductItem();
		item.setPk(DynamoKeys.franchisePk(product.franchiseId()));
		item.setSk(DynamoKeys.productSk(product.branchId(), product.id()));
		item.setName(newName);
		return item;
	}

	private static Throwable translate(TransactionCanceledException exception, Product current, String newName,
			boolean reservationMoves) {
		if (DynamoConditions.failedAt(exception, BRANCH_CHECK)) {
			return new BusinessException(FranchiseError.BRANCH_NOT_FOUND, current.branchId());
		}
		if (reservationMoves && DynamoConditions.failedAt(exception, NEW_RESERVATION)) {
			return new BusinessException(FranchiseError.PRODUCT_ALREADY_EXISTS, newName);
		}
		return new ProductChangedException(exception);
	}

}
