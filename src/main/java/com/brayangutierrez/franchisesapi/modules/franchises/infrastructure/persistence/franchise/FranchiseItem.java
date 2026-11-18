package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.franchise;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.BaseItem;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * Ítem de franquicia en la tabla única: {@code PK=F#{id}}, {@code SK=META}.
 */
@DynamoDbBean
public class FranchiseItem extends BaseItem {

	public static final String ENTITY_TYPE = "FRANCHISE";

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
