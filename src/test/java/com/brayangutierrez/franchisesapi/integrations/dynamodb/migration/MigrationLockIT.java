package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/** Lock distribuido contra dynamodb-test; cada test usa su propia partición. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MigrationLockIT {

	private static final Duration TTL = Duration.ofMinutes(1);
	private static final Duration POLL = Duration.ofMillis(50);

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	private final String partition = "SCHEMA#MIGRATIONS#test-" + UUID.randomUUID();

	@Test
	void anotherInstanceCannotTakeALiveLock() {
		verify(lock(TTL, Clock.systemUTC()).tryAcquire(), true);
		verify(lock(TTL, Clock.systemUTC()).tryAcquire(), false);
	}

	@Test
	void releasedLockCanBeTakenAgain() {
		MigrationLock first = lock(TTL, Clock.systemUTC());
		verify(first.tryAcquire(), true);
		StepVerifier.create(first.release()).verifyComplete();

		verify(lock(TTL, Clock.systemUTC()).tryAcquire(), true);
	}

	@Test
	void expiredLockCanBeTakenOverAndTheOldOwnerCannotReleaseIt() {
		MigrationLock crashed = lock(TTL, Clock.systemUTC());
		verify(crashed.tryAcquire(), true);
		MigrationLock later = lock(TTL, Clock.offset(Clock.systemUTC(), TTL.multipliedBy(2)));

		verify(later.tryAcquire(), true);
		StepVerifier.create(crashed.release()).verifyComplete();
		verify(lock(TTL, Clock.offset(Clock.systemUTC(), TTL.multipliedBy(2))).tryAcquire(), false);
	}

	@Test
	void acquireWaitsUntilTheOwnerReleasesIt() {
		MigrationLock owner = lock(TTL, Clock.systemUTC());
		verify(owner.tryAcquire(), true);

		Mono<Void> waiting = lock(TTL, Clock.systemUTC()).acquire();
		Mono<Void> releaseLater = Mono.delay(Duration.ofMillis(300)).then(owner.release());

		StepVerifier.create(Mono.when(waiting, releaseLater)).verifyComplete();
	}

	@Test
	void acquireGivesUpWhileAnotherInstanceKeepsTheLock() {
		verify(lock(TTL, Clock.systemUTC()).tryAcquire(), true);
		MigrationLock impatient = lock(Duration.ofMillis(200), Clock.systemUTC());

		StepVerifier.create(impatient.acquire())
				.expectErrorSatisfies(error -> assertThat(error)
						.isInstanceOf(MigrationException.class)
						.hasMessageContaining("lock de migraciones"))
				.verify(Duration.ofSeconds(5));
	}

	private MigrationLock lock(Duration ttl, Clock clock) {
		return new MigrationLock(client, properties.tableName(), partition, ttl, POLL, clock);
	}

	private static void verify(Mono<Boolean> attempt, boolean expected) {
		StepVerifier.create(attempt).expectNext(expected).verifyComplete();
	}

}
