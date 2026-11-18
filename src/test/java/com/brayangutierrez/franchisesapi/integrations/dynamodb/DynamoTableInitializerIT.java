package com.brayangutierrez.franchisesapi.integrations.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DynamoTableInitializerIT {

	@Autowired
	private DynamoTableInitializer initializer;

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void tableIsCreatedOnStartupWithSingleTableSchema() {
		TableDescription table = client.describeTable(r -> r.tableName(properties.tableName())).join().table();

		assertThat(table.keySchema())
				.anySatisfy(key -> {
					assertThat(key.attributeName()).isEqualTo("PK");
					assertThat(key.keyType()).isEqualTo(KeyType.HASH);
				})
				.anySatisfy(key -> {
					assertThat(key.attributeName()).isEqualTo("SK");
					assertThat(key.keyType()).isEqualTo(KeyType.RANGE);
				});
		assertThat(table.billingModeSummary().billingMode()).isEqualTo(BillingMode.PAY_PER_REQUEST);
	}

	@Test
	void runningTheInitializerAgainIsHarmless() {
		StepVerifier.create(initializer.createTableIfMissing())
				.verifyComplete();
	}

}
