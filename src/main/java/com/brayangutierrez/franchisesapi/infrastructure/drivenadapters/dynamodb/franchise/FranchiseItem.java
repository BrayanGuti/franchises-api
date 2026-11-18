package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.franchise;

import com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.BaseItem;

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
