package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.BaseItem;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * Ítem de sucursal en la tabla única: {@code PK=F#{franchiseId}}, {@code SK=B#{branchId}}.
 */
@DynamoDbBean
public class BranchItem extends BaseItem {

	public static final String ENTITY_TYPE = "BRANCH";

	private String name;
	private String status;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/** Vacío o {@code ACTIVE}: activa. {@code DELETING}: se está eliminando (ver {@link BranchConditions}). */
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
