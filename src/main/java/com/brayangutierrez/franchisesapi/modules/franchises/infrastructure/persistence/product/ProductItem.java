package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.BaseItem;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * Ítem de producto en la tabla única: {@code PK=F#{franchiseId}}, {@code SK=B#{branchId}#P#{productId}}.
 * Es un ítem independiente (no una lista dentro de la sucursal): cada producto se modifica sin
 * reescribir la sucursal.
 */
@DynamoDbBean
public class ProductItem extends BaseItem {

	public static final String ENTITY_TYPE = "PRODUCT";

	private String name;
	private Integer stock;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

}
