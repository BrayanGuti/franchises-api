package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.branch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.brayangutierrez.franchisesapi.domain.model.branch.Branch;

class BranchItemMapperTest {

	private final Branch branch = new Branch("b1", "f1", "Sucursal Centro");

	@Test
	void mapsBranchToTableItemInsideItsFranchisePartition() {
		BranchItem item = BranchItemMapper.toItem(branch);

		assertThat(item.getPk()).isEqualTo("F#f1");
		assertThat(item.getSk()).isEqualTo("B#b1");
		assertThat(item.getEntityType()).isEqualTo(BranchItem.ENTITY_TYPE);
		assertThat(item.getName()).isEqualTo("Sucursal Centro");
	}

	@Test
	void mapsTableItemBackToTheSameBranch() {
		assertThat(BranchItemMapper.toDomain(BranchItemMapper.toItem(branch))).isEqualTo(branch);
	}

}
