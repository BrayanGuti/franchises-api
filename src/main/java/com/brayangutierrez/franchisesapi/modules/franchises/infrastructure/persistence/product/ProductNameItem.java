package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.BaseItem;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * Reserva del nombre de un producto dentro de su sucursal:
 * {@code PK=F#{franchiseId}}, {@code SK=B#{branchId}#NAME#{nombre normalizado}}.
 * <p>
 * Existe solo para garantizar la unicidad del nombre: se crea, cambia y borra en la misma
 * transacción que su producto. Guarda el ID del producto que la ocupa.
 */
@DynamoDbBean
public class ProductNameItem extends BaseItem {

	public static final String ENTITY_TYPE = "PRODUCT_NAME";

	private String productId;

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

}
