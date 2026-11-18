package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;

/**
 * Convierte entre el modelo de dominio {@link Product} y sus ítems en la tabla.
 */
final class ProductItemMapper {

	private ProductItemMapper() {
	}

	static ProductItem toItem(Product product) {
		ProductItem item = new ProductItem();
		item.setPk(DynamoKeys.franchisePk(product.franchiseId()));
		item.setSk(DynamoKeys.productSk(product.branchId(), product.id()));
		item.setEntityType(ProductItem.ENTITY_TYPE);
		item.setName(product.name());
		item.setStock(product.stock());
		return item;
	}

	static ProductNameItem toNameItem(Product product) {
		ProductNameItem item = new ProductNameItem();
		item.setPk(DynamoKeys.franchisePk(product.franchiseId()));
		item.setSk(DynamoKeys.productNameSk(product.branchId(), product.name()));
		item.setEntityType(ProductNameItem.ENTITY_TYPE);
		item.setProductId(product.id());
		return item;
	}

	static Product toDomain(ProductItem item) {
		return new Product(
				DynamoKeys.productIdFrom(item.getSk()),
				DynamoKeys.franchiseIdFrom(item.getPk()),
				DynamoKeys.branchIdFromProductSk(item.getSk()),
				item.getName(),
				item.getStock());
	}

}
