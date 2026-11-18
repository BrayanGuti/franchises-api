package com.brayangutierrez.franchisesapi.integrations.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

class DynamoConditionsTest {

	private static final CancellationReason NONE = CancellationReason.builder().code("None").build();
	private static final CancellationReason FAILED = CancellationReason.builder().code("ConditionalCheckFailed").build();

	@Test
	void identifiesWhichOperationFailed() {
		TransactionCanceledException exception = canceled(NONE, FAILED);

		assertThat(DynamoConditions.failedAt(exception, 0)).isFalse();
		assertThat(DynamoConditions.failedAt(exception, 1)).isTrue();
	}

	@Test
	void positionOutsideTheTransactionIsNotAFailure() {
		assertThat(DynamoConditions.failedAt(canceled(FAILED), 5)).isFalse();
	}

	@Test
	void otherCancellationCodesAreNotConditionFailures() {
		CancellationReason throttled = CancellationReason.builder().code("ThrottlingError").build();

		assertThat(DynamoConditions.failedAt(canceled(throttled), 0)).isFalse();
	}

	private static TransactionCanceledException canceled(CancellationReason... reasons) {
		return TransactionCanceledException.builder().cancellationReasons(reasons).build();
	}

}
