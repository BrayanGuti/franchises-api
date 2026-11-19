package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

/**
 * Arma el runner de migraciones con todas las {@link Migration} registradas como beans.
 * Con {@code app.migrations.enabled=false} no se crea nada y la app arranca sin migrar.
 */
@Configuration
@ConditionalOnBooleanProperty(name = "app.migrations.enabled", matchIfMissing = true)
class MigrationConfig {

	/** Partición del historial y del lock; no coincide con ninguna clave de negocio ({@code F#...}). */
	static final String PARTITION_KEY = "SCHEMA#MIGRATIONS";
	private static final Duration LOCK_POLL_INTERVAL = Duration.ofSeconds(2);

	@Bean
	MigrationRunner migrationRunner(DynamoDbAsyncClient client, DynamoDbProperties dynamo,
			MigrationProperties properties, ObjectProvider<Migration> migrations) {
		Clock clock = Clock.systemUTC();
		String table = dynamo.tableName();
		return new MigrationRunner(migrations.orderedStream().toList(),
				new MigrationLock(client, table, PARTITION_KEY, properties.lockTtl(), LOCK_POLL_INTERVAL, clock),
				new MigrationHistory(client, table, PARTITION_KEY, clock),
				clock);
	}

	@Bean
	MigrationStatus migrationStatus() {
		return new MigrationStatus();
	}

	@Bean
	MigrationStartupRunner migrationStartupRunner(MigrationRunner runner, MigrationStatus status) {
		return new MigrationStartupRunner(runner, status);
	}

}
