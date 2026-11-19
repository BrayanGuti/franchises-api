package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/**
 * Runner contra dynamodb-test. Cada test usa su propia partición de historial, así que no choca con
 * el historial real que escribe el arranque ni con los demás tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MigrationRunnerIT {

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	private final String partition = "SCHEMA#MIGRATIONS#test-" + UUID.randomUUID();
	private final Map<String, AtomicInteger> executions = new ConcurrentHashMap<>();
	private final List<String> order = new CopyOnWriteArrayList<>();

	@Test
	void appliesPendingMigrationsInVersionOrderAndRecordsThem() {
		StepVerifier.create(runner(tracked("V002__Second"), tracked("V001__First")).run())
				.assertNext(result -> {
					assertThat(result.applied()).containsExactly("V001", "V002");
					assertThat(result.version()).isEqualTo("V002");
				})
				.verifyComplete();

		assertThat(order).containsExactly("V001__First", "V002__Second");
		assertThat(history().appliedVersions().block()).containsExactlyInAnyOrder("V001", "V002");
	}

	@Test
	void runningAgainAppliesNothing() {
		runner(tracked("V001__First")).run().block();

		StepVerifier.create(runner(tracked("V001__First")).run())
				.assertNext(result -> assertThat(result.applied()).isEmpty())
				.verifyComplete();
		assertThat(executions.get("V001__First")).hasValue(1);
	}

	@Test
	void onlyNewMigrationsRunAfterAnUpgrade() {
		runner(tracked("V001__First")).run().block();

		StepVerifier.create(runner(tracked("V001__First"), tracked("V002__Second")).run())
				.assertNext(result -> assertThat(result.applied()).containsExactly("V002"))
				.verifyComplete();
		assertThat(executions.get("V001__First")).hasValue(1);
	}

	@Test
	void failureStopsTheRunKeepsPreviousVersionsAndReleasesTheLock() {
		Migration broken = new TestMigration("V002__Broken", () -> Mono.error(new IllegalStateException("boom")));

		StepVerifier.create(runner(tracked("V001__First"), broken, tracked("V003__Third")).run())
				.expectErrorSatisfies(error -> assertThat(error)
						.isInstanceOf(MigrationException.class)
						.hasMessageContaining("V002__Broken")
						.hasRootCauseMessage("boom"))
				.verify();
		assertThat(history().appliedVersions().block()).containsExactly("V001");
		assertThat(executions).doesNotContainKey("V003__Third");

		StepVerifier.create(runner(tracked("V001__First"), tracked("V002__Fixed"), tracked("V003__Third")).run())
				.assertNext(result -> assertThat(result.applied()).containsExactly("V002", "V003"))
				.verifyComplete();
	}

	@Test
	void concurrentInstancesApplyEachMigrationExactlyOnce() {
		List<MigrationResult> results = Flux.range(0, 5)
				.flatMap(instance -> runner(slow("V001__First"), slow("V002__Second")).run(), 5)
				.collectList()
				.block(Duration.ofSeconds(30));

		assertThat(executions.get("V001__First")).hasValue(1);
		assertThat(executions.get("V002__Second")).hasValue(1);
		assertThat(results).hasSize(5).allSatisfy(result -> assertThat(result.version()).isEqualTo("V002"));
		assertThat(results).flatExtracting(MigrationResult::applied).containsExactlyInAnyOrder("V001", "V002");
	}

	@Test
	void versionsRecordedByANewerReleaseAreIgnored() {
		history().record(MigrationVersion.parse("V005__FromTheFuture"), Duration.ZERO).block();

		StepVerifier.create(runner(tracked("V001__First")).run())
				.assertNext(result -> {
					assertThat(result.applied()).containsExactly("V001");
					assertThat(result.version()).isEqualTo("V001");
				})
				.verifyComplete();
	}

	@Test
	void withoutMigrationsThereIsNoVersion() {
		StepVerifier.create(runner().run())
				.assertNext(result -> {
					assertThat(result.applied()).isEmpty();
					assertThat(result.version()).isNull();
				})
				.verifyComplete();
	}

	private Migration tracked(String name) {
		return new TestMigration(name, () -> Mono.fromRunnable(() -> {
			order.add(name);
			executions.computeIfAbsent(name, key -> new AtomicInteger()).incrementAndGet();
		}));
	}

	/** Tarda lo suficiente para que las demás instancias choquen con el lock tomado. */
	private Migration slow(String name) {
		Migration tracked = tracked(name);
		return new TestMigration(name, () -> Mono.delay(Duration.ofMillis(200)).then(tracked.migrate()));
	}

	private MigrationRunner runner(Migration... migrations) {
		Clock clock = Clock.systemUTC();
		MigrationLock lock = new MigrationLock(client, properties.tableName(), partition,
				Duration.ofSeconds(30), Duration.ofMillis(50), clock);
		return new MigrationRunner(List.of(migrations), lock, history(), clock);
	}

	private MigrationHistory history() {
		return new MigrationHistory(client, properties.tableName(), partition, Clock.systemUTC());
	}

}
