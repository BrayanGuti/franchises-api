package com.brayangutierrez.franchisesapi.integrations.dynamodb;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Crea la tabla al arrancar si no existe. Solo se activa con {@code app.dynamodb.create-table=true}
 * (perfiles dev y test); en AWS la tabla la crea Terraform.
 * <p>
 * Es idempotente: si la tabla ya existe no hace nada. Bloquear aquí es aceptable porque ocurre
 * una sola vez al arrancar, antes de recibir tráfico, nunca dentro de una petición.
 */
@Component
@ConditionalOnBooleanProperty("app.dynamodb.create-table")
@Order(DynamoTableInitializer.ORDER)
public class DynamoTableInitializer implements ApplicationRunner {

	/** Corre antes que cualquier otro arranque que use la tabla (por ejemplo, las migraciones). */
	public static final int ORDER = 0;

	private static final Logger log = LoggerFactory.getLogger(DynamoTableInitializer.class);
	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	private final DynamoDbAsyncClient client;
	private final String tableName;

	public DynamoTableInitializer(DynamoDbAsyncClient client, DynamoDbProperties properties) {
		this.client = client;
		this.tableName = properties.tableName();
	}

	@Override
	public void run(ApplicationArguments args) {
		createTableIfMissing().block(TIMEOUT);
	}

	Mono<Void> createTableIfMissing() {
		return Mono.fromFuture(() -> client.createTable(createTableRequest()))
				.doOnSuccess(response -> log.info("Tabla '{}' creada", tableName))
				.then()
				.onErrorResume(ResourceInUseException.class, alreadyExists -> {
					log.info("La tabla '{}' ya existe", tableName);
					return Mono.empty();
				})
				.then(Mono.fromFuture(() -> client.waiter().waitUntilTableExists(r -> r.tableName(tableName))))
				.then();
	}

	private CreateTableRequest createTableRequest() {
		return CreateTableRequest.builder()
				.tableName(tableName)
				.billingMode(BillingMode.PAY_PER_REQUEST)
				.attributeDefinitions(
						attribute("PK"),
						attribute("SK"))
				.keySchema(
						KeySchemaElement.builder().attributeName("PK").keyType(KeyType.HASH).build(),
						KeySchemaElement.builder().attributeName("SK").keyType(KeyType.RANGE).build())
				.build();
	}

	private static AttributeDefinition attribute(String name) {
		return AttributeDefinition.builder().attributeName(name).attributeType(ScalarAttributeType.S).build();
	}

}
