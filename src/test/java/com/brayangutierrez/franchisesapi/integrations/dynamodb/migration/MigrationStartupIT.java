package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Al arrancar, la app aplica la baseline, la registra, suelta el lock y publica su versión. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class MigrationStartupIT {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void baselineIsRecordedInTheHistory() {
		MigrationHistory history = new MigrationHistory(client, properties.tableName(),
				MigrationConfig.PARTITION_KEY, Clock.systemUTC());

		assertThat(history.appliedVersions().block()).contains("V001");
	}

	@Test
	void lockIsReleasedAfterStartup() {
		Map<String, AttributeValue> key = Map.of(
				"PK", AttributeValue.fromS(MigrationConfig.PARTITION_KEY),
				"SK", AttributeValue.fromS("LOCK"));

		boolean lockExists = client.getItem(request -> request
				.tableName(properties.tableName()).key(key).consistentRead(true)).join().hasItem();

		assertThat(lockExists).isFalse();
	}

	@Test
	void infoEndpointPublishesTheVerifiedVersion() {
		webTestClient.get().uri("/actuator/info")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.migrations.version").isEqualTo("V001");
	}

}
