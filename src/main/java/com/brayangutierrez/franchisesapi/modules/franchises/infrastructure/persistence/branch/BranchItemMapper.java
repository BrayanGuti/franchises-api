package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;

/**
 * Convierte entre el modelo de dominio {@link Branch} y su ítem en la tabla.
 */
public final class BranchItemMapper {

	private BranchItemMapper() {
	}

	static BranchItem toItem(Branch branch) {
		BranchItem item = new BranchItem();
		item.setPk(DynamoKeys.franchisePk(branch.franchiseId()));
		item.setSk(DynamoKeys.branchSk(branch.id()));
		item.setEntityType(BranchItem.ENTITY_TYPE);
		item.setName(branch.name());
		return item;
	}

	public static Branch toDomain(BranchItem item) {
		return new Branch(
				DynamoKeys.branchIdFrom(item.getSk()),
				DynamoKeys.franchiseIdFrom(item.getPk()),
				item.getName());
	}

}
