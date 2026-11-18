package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.enhanced.dynamodb.Key;

class DynamoKeysTest {

	@Test
	void franchiseKeysFollowTheSingleTableFormat() {
		assertThat(DynamoKeys.franchisePk("f1")).isEqualTo("F#f1");
		assertThat(DynamoKeys.FRANCHISE_SK).isEqualTo("META");
	}

	@Test
	void branchAndProductSortKeysShareTheBranchPrefix() {
		assertThat(DynamoKeys.branchSk("b1")).isEqualTo("B#b1");
		assertThat(DynamoKeys.productSk("b1", "p1")).isEqualTo("B#b1#P#p1");
		assertThat(DynamoKeys.productSk("b1", "p1")).startsWith(DynamoKeys.BRANCH_PREFIX);
	}

	@Test
	void franchiseKeyCombinesPartitionAndSortKey() {
		Key key = DynamoKeys.franchiseKey("f1");

		assertThat(key.partitionKeyValue().s()).isEqualTo("F#f1");
		assertThat(key.sortKeyValue()).hasValueSatisfying(sk -> assertThat(sk.s()).isEqualTo("META"));
	}

	@Test
	void branchKeyLivesInsideTheFranchisePartition() {
		Key key = DynamoKeys.branchKey("f1", "b1");

		assertThat(key.partitionKeyValue().s()).isEqualTo("F#f1");
		assertThat(key.sortKeyValue()).hasValueSatisfying(sk -> assertThat(sk.s()).isEqualTo("B#b1"));
	}

	@Test
	void idsAreExtractedFromTheirKeys() {
		assertThat(DynamoKeys.franchiseIdFrom("F#9a7e")).isEqualTo("9a7e");
		assertThat(DynamoKeys.branchIdFrom("B#4c1d")).isEqualTo("4c1d");
		assertThat(DynamoKeys.branchIdFromProductSk("B#4c1d#P#77ab")).isEqualTo("4c1d");
		assertThat(DynamoKeys.productIdFrom("B#4c1d#P#77ab")).isEqualTo("77ab");
	}

	@Test
	void equivalentProductNamesShareTheSameReservation() {
		String reservation = DynamoKeys.productNameSk("b1", "Café Americano");

		assertThat(reservation).isEqualTo("B#b1#NAME#cafe americano");
		assertThat(DynamoKeys.productNameSk("b1", "  CAFE   americano ")).isEqualTo(reservation);
		assertThat(DynamoKeys.productNameSk("b1", "café\tAMERICANO")).isEqualTo(reservation);
	}

	@Test
	void differentNamesOrBranchesUseDifferentReservations() {
		assertThat(DynamoKeys.productNameSk("b1", "Café"))
				.isNotEqualTo(DynamoKeys.productNameSk("b1", "Té"))
				.isNotEqualTo(DynamoKeys.productNameSk("b2", "Café"));
	}

}
