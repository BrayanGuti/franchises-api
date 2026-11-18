package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.branch;

import com.brayangutierrez.franchisesapi.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.DynamoKeys;

/**
 * Convierte entre el modelo de dominio {@link Branch} y su ítem en la tabla.
 */
final class BranchItemMapper {

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

	static Branch toDomain(BranchItem item) {
		return new Branch(
				DynamoKeys.branchIdFrom(item.getSk()),
				DynamoKeys.franchiseIdFrom(item.getPk()),
				item.getName());
	}

}
