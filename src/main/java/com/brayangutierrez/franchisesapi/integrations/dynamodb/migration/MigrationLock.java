package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Lock distribuido para que una sola instancia migre a la vez (DynamoDB no tiene locks).
 * <p>
 * Es un ítem {@code PK=<partición de migraciones>, SK=LOCK} con su dueño y vencimiento. Se toma con
 * un {@code PutItem} condicional: solo si no existe o si ya venció. El vencimiento evita que una
 * instancia que murió con el lock tomado bloquee los despliegues para siempre; por eso las
 * migraciones deben ser idempotentes: si una tarda más que el vencimiento, otra puede retomarla.
 */
public class MigrationLock {

	private static final Logger log = LoggerFactory.getLogger(MigrationLock.class);
	private static final String LOCK_SK = "LOCK";

	private final DynamoDbAsyncClient client;
	private final String tableName;
	private final String partitionKey;
	private final Duration ttl;
	private final Duration pollInterval;
	private final Clock clock;
	private final String owner = UUID.randomUUID().toString();

	public MigrationLock(DynamoDbAsyncClient client, String tableName, String partitionKey,
			Duration ttl, Duration pollInterval, Clock clock) {
		this.client = client;
		this.tableName = tableName;
		this.partitionKey = partitionKey;
		this.ttl = ttl;
		this.pollInterval = pollInterval;
		this.clock = clock;
	}

	/**
	 * Espera hasta tomar el lock. Se rinde después del vencimiento más un intervalo: para entonces
	 * un lock abandonado ya venció, así que seguir sin poder tomarlo indica que otra instancia
	 * sigue migrando activamente.
	 */
	public Mono<Void> acquire() {
		Duration maxWait = ttl.plus(pollInterval);
		return Mono.defer(this::tryAcquire)
				.filter(Boolean::booleanValue)
				.repeatWhenEmpty(attempts -> attempts.delayElements(pollInterval))
				.timeout(maxWait)
				.onErrorMap(TimeoutException.class, timeout -> new MigrationException(
						"No se pudo obtener el lock de migraciones en " + maxWait.toSeconds() + " s"))
				.then();
	}

	/** Intenta tomar el lock una vez: {@code true} si quedó a nombre de esta instancia. */
	public Mono<Boolean> tryAcquire() {
		long now = clock.millis();
		return Mono.fromFuture(() -> client.putItem(request -> request
				.tableName(tableName)
				.item(Map.of(
						"PK", s(partitionKey),
						"SK", s(LOCK_SK),
						"entityType", s("MIGRATION_LOCK"),
						"owner", s(owner),
						"expiresAt", n(now + ttl.toMillis())))
				.conditionExpression("attribute_not_exists(PK) OR expiresAt < :now")
				.expressionAttributeValues(Map.of(":now", n(now)))))
				.thenReturn(true)
				.onErrorReturn(ConditionalCheckFailedException.class, false);
	}

	/** Libera el lock solo si sigue siendo de esta instancia (si venció, otra pudo tomarlo). */
	public Mono<Void> release() {
		return Mono.fromFuture(() -> client.deleteItem(request -> request
				.tableName(tableName)
				.key(Map.of("PK", s(partitionKey), "SK", s(LOCK_SK)))
				.conditionExpression("#owner = :owner")
				.expressionAttributeNames(Map.of("#owner", "owner"))
				.expressionAttributeValues(Map.of(":owner", s(owner)))))
				.then()
				.onErrorResume(ConditionalCheckFailedException.class, lost -> {
					log.warn("El lock de migraciones venció antes de liberarlo; otra instancia pudo tomarlo");
					return Mono.empty();
				});
	}

	private static AttributeValue s(String value) {
		return AttributeValue.fromS(value);
	}

	private static AttributeValue n(long value) {
		return AttributeValue.fromN(Long.toString(value));
	}

}
