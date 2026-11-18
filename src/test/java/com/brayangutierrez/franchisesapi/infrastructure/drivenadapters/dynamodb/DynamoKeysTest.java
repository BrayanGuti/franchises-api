package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb;

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
	void franchiseIdIsExtractedFromPartitionKey() {
		assertThat(DynamoKeys.franchiseIdFrom("F#9a7e")).isEqualTo("9a7e");
	}

}
