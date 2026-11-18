package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import org.springframework.stereotype.Component;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.DynamoConditions;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Cambia solo el stock de un producto con un {@code UpdateItem}.
 * <ul>
 * <li>El ítem parcial lleva solo las claves y el stock, y los nulos se ignoran: la operación
 * resultante es {@code SET stock = :stock}, así que un renombrado simultáneo no se pisa.</li>
 * <li>{@code UpdateItem} crea el ítem si no existe; la condición {@code attribute_exists(PK)}
 * lo impide, para que un PATCH sobre un producto borrado no lo resucite como fantasma.</li>
 * </ul>
 */
@Component
class UpdateProductStockOperation {

	private final ProductTables tables;

	UpdateProductStockOperation(ProductTables tables) {
		this.tables = tables;
	}

	Mono<Product> execute(String franchiseId, String branchId, String productId, int stock) {
		ProductItem onlyStock = new ProductItem();
		onlyStock.setPk(DynamoKeys.franchisePk(franchiseId));
		onlyStock.setSk(DynamoKeys.productSk(branchId, productId));
		onlyStock.setStock(stock);

		UpdateItemEnhancedRequest<ProductItem> request = UpdateItemEnhancedRequest.builder(ProductItem.class)
				.item(onlyStock)
				.ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
				.conditionExpression(DynamoConditions.ITEM_EXISTS)
				.build();
		return Mono.fromFuture(() -> tables.products().updateItem(request))
				.map(ProductItemMapper::toDomain)
				.onErrorMap(ConditionalCheckFailedException.class,
						e -> new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, productId));
	}

}
