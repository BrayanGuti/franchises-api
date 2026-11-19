package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import org.springframework.stereotype.Component;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.DynamoConditions;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch.BranchConditions;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Alta de un producto en una sola transacción: la sucursal debe existir y estar activa (no en
 * proceso de eliminación), el nombre no debe estar reservado y el producto no debe existir. Si
 * cualquiera falla, DynamoDB no aplica ninguna escritura: no quedan productos huérfanos ni reservas
 * sueltas.
 */
@Component
class AddProductOperation {

	/** Posición de cada operación dentro de la transacción. */
	private static final int BRANCH_CHECK = 0;
	private static final int NAME_RESERVATION = 1;
	private static final int PRODUCT_PUT = 2;

	private final ProductTables tables;

	AddProductOperation(ProductTables tables) {
		this.tables = tables;
	}

	Mono<Product> execute(Product product) {
		TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
				.addConditionCheck(tables.products(), ConditionCheck.builder()
						.key(DynamoKeys.branchKey(product.franchiseId(), product.branchId()))
						.conditionExpression(BranchConditions.existsAndActive())
						.build())
				.addPutItem(tables.productNames(), TransactPutItemEnhancedRequest.builder(ProductNameItem.class)
						.item(ProductItemMapper.toNameItem(product))
						.conditionExpression(DynamoConditions.ITEM_NOT_EXISTS)
						.build())
				.addPutItem(tables.products(), TransactPutItemEnhancedRequest.builder(ProductItem.class)
						.item(ProductItemMapper.toItem(product))
						.conditionExpression(DynamoConditions.ITEM_NOT_EXISTS)
						.build())
				.build();
		return Mono.fromFuture(() -> tables.client().transactWriteItems(request))
				.thenReturn(product)
				.onErrorMap(TransactionCanceledException.class, e -> translate(e, product));
	}

	private static Throwable translate(TransactionCanceledException exception, Product product) {
		if (DynamoConditions.failedAt(exception, BRANCH_CHECK)) {
			return new BusinessException(FranchiseError.BRANCH_NOT_FOUND, product.branchId());
		}
		if (DynamoConditions.failedAt(exception, NAME_RESERVATION)) {
			return new BusinessException(FranchiseError.PRODUCT_ALREADY_EXISTS, product.name());
		}
		if (DynamoConditions.failedAt(exception, PRODUCT_PUT)) {
			return new BusinessException(FranchiseError.PRODUCT_ALREADY_EXISTS, product.id());
		}
		return exception;
	}

}
