package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import org.springframework.stereotype.Component;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactDeleteItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Borra un producto y su reserva de nombre en una sola transacción.
 * <p>
 * La clave de la reserva depende del nombre, así que primero se lee el producto. Para que nadie
 * lo cambie entre la lectura y el borrado, la transacción exige que el nombre siga siendo el
 * leído; si cambió, se vuelve a leer y se reintenta. La reserva solo se borra si sigue apuntando
 * a este producto (o si falta), así que nunca se libera el nombre de otro producto.
 */
@Component
class RemoveProductOperation {

	static final int MAX_ATTEMPTS = 3;

	private final ProductTables tables;

	RemoveProductOperation(ProductTables tables) {
		this.tables = tables;
	}

	Mono<Void> execute(String franchiseId, String branchId, String productId) {
		return Mono.defer(() -> read(franchiseId, branchId, productId)
				.switchIfEmpty(Mono.error(() -> new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, productId)))
				.flatMap(this::deleteWithReservation))
				.retryWhen(Retry.max(MAX_ATTEMPTS - 1L)
						.filter(ProductChangedException.class::isInstance)
						.onRetryExhaustedThrow((spec, signal) ->
								new BusinessException(FranchiseError.PRODUCT_MODIFIED_CONCURRENTLY, productId)));
	}

	private Mono<ProductItem> read(String franchiseId, String branchId, String productId) {
		GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
				.key(DynamoKeys.productKey(franchiseId, branchId, productId))
				.consistentRead(true)
				.build();
		return Mono.fromFuture(() -> tables.products().getItem(request));
	}

	private Mono<Void> deleteWithReservation(ProductItem item) {
		String franchiseId = DynamoKeys.franchiseIdFrom(item.getPk());
		String branchId = DynamoKeys.branchIdFromProductSk(item.getSk());
		String productId = DynamoKeys.productIdFrom(item.getSk());
		TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
				.addDeleteItem(tables.products(), TransactDeleteItemEnhancedRequest.builder()
						.key(DynamoKeys.productKey(franchiseId, branchId, productId))
						.conditionExpression(ProductConditions.existsWithName(item.getName()))
						.build())
				.addDeleteItem(tables.productNames(), TransactDeleteItemEnhancedRequest.builder()
						.key(DynamoKeys.productNameKey(franchiseId, branchId, item.getName()))
						.conditionExpression(ProductConditions.reservationMissingOrOwnedBy(productId))
						.build())
				.build();
		return Mono.fromFuture(() -> tables.client().transactWriteItems(request))
				.onErrorMap(TransactionCanceledException.class, ProductChangedException::new);
	}

}
