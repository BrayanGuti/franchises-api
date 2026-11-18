package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.franchise;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;

/**
 * Convierte entre el modelo de dominio {@link Franchise} y su ítem en la tabla.
 */
public final class FranchiseItemMapper {

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

	public static Franchise toDomain(FranchiseItem item) {
		return new Franchise(DynamoKeys.franchiseIdFrom(item.getPk()), item.getName());
	}

}
