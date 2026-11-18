package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.franchise;

import com.brayangutierrez.franchisesapi.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.DynamoKeys;

/**
 * Convierte entre el modelo de dominio {@link Franchise} y su ítem en la tabla.
 */
final class FranchiseItemMapper {

	private FranchiseItemMapper() {
	}

	static FranchiseItem toItem(Franchise franchise) {
		FranchiseItem item = new FranchiseItem();
		item.setPk(DynamoKeys.franchisePk(franchise.id()));
		item.setSk(DynamoKeys.FRANCHISE_SK);
		item.setEntityType(FranchiseItem.ENTITY_TYPE);
		item.setName(franchise.name());
		return item;
	}

	static Franchise toDomain(FranchiseItem item) {
		return new Franchise(DynamoKeys.franchiseIdFrom(item.getPk()), item.getName());
	}

}
