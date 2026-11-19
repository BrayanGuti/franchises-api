package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Historial de migraciones aplicadas, guardado en la misma tabla: un ítem por versión con
 * {@code PK=<partición de migraciones>, SK=V001}. Ninguna consulta de negocio lee esta partición.
 */
public class MigrationHistory {

	private static final String VERSION_PREFIX = "V";

	private final DynamoDbAsyncClient client;
	private final String tableName;
	private final String partitionKey;
	private final Clock clock;

	public MigrationHistory(DynamoDbAsyncClient client, String tableName, String partitionKey, Clock clock) {
		this.client = client;
		this.tableName = tableName;
		this.partitionKey = partitionKey;
		this.clock = clock;
	}

	/** Versiones ya aplicadas (lectura consistente: se acaba de tomar el lock). */
	public Mono<Set<String>> appliedVersions() {
		return Flux.from(client.queryPaginator(request -> request
				.tableName(tableName)
				.consistentRead(true)
				.keyConditionExpression("PK = :pk AND begins_with(SK, :prefix)")
				.expressionAttributeValues(Map.of(
						":pk", AttributeValue.fromS(partitionKey),
						":prefix", AttributeValue.fromS(VERSION_PREFIX))))
				.items())
				.map(item -> item.get("SK").s())
				.collect(Collectors.toUnmodifiableSet());
	}

	/** Registra una versión aplicada. Si ya estaba registrada (reintento), no la sobrescribe. */
	public Mono<Void> record(MigrationVersion version, Duration duration) {
		return Mono.fromFuture(() -> client.putItem(request -> request
				.tableName(tableName)
				.item(Map.of(
						"PK", AttributeValue.fromS(partitionKey),
						"SK", AttributeValue.fromS(version.label()),
						"entityType", AttributeValue.fromS("MIGRATION"),
						"description", AttributeValue.fromS(version.description()),
						"appliedAt", AttributeValue.fromS(clock.instant().toString()),
						"durationMs", AttributeValue.fromN(Long.toString(duration.toMillis()))))
				.conditionExpression("attribute_not_exists(PK)")))
				.then()
				.onErrorResume(ConditionalCheckFailedException.class, alreadyRecorded -> Mono.empty());
	}

}
