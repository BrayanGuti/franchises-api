package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.franchise;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;

class FranchiseItemMapperTest {

	@Test
	void mapsFranchiseToTableItem() {
		FranchiseItem item = FranchiseItemMapper.toItem(new Franchise("f1", "Cafeterías Norte"));

		assertThat(item.getPk()).isEqualTo("F#f1");
		assertThat(item.getSk()).isEqualTo("META");
		assertThat(item.getEntityType()).isEqualTo(FranchiseItem.ENTITY_TYPE);
		assertThat(item.getName()).isEqualTo("Cafeterías Norte");
	}

	@Test
	void mapsTableItemBackToTheSameFranchise() {
		Franchise original = new Franchise("f1", "Cafeterías Norte");

		Franchise mapped = FranchiseItemMapper.toDomain(FranchiseItemMapper.toItem(original));

		assertThat(mapped).isEqualTo(original);
	}

}
