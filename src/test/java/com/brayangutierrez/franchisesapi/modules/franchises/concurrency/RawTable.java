package com.brayangutierrez.franchisesapi.modules.franchises.concurrency;

import java.util.List;
import java.util.Map;

import org.springframework.boot.test.context.TestComponent;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Lee el estado final directamente de DynamoDB, con lectura consistente, sin pasar por la API
 * que se está probando.
 */
@TestComponent
class RawTable {

	private final DynamoDbAsyncClient client;
	private final String tableName;

	RawTable(DynamoDbAsyncClient client, DynamoDbProperties properties) {
		this.client = client;
		this.tableName = properties.tableName();
	}

	/** Ítems de la franquicia cuya sort key empieza por {@code skPrefix}. */
	List<Map<String, AttributeValue>> items(String franchiseId, String skPrefix) {
		return client.query(request -> request
				.tableName(tableName)
				.consistentRead(true)
				.keyConditionExpression("PK = :pk AND begins_with(SK, :prefix)")
				.expressionAttributeValues(Map.of(
						":pk", AttributeValue.fromS("F#" + franchiseId),
						":prefix", AttributeValue.fromS(skPrefix))))
				.join()
				.items();
	}

	List<Map<String, AttributeValue>> itemsOfType(String franchiseId, String branchId, String entityType) {
		return items(franchiseId, "B#" + branchId).stream()
				.filter(item -> entityType.equals(item.get("entityType").s()))
				.toList();
	}

	void put(Map<String, AttributeValue> item) {
		client.putItem(request -> request.tableName(tableName).item(item)).join();
	}

	void delete(String pk, String sk) {
		client.deleteItem(request -> request.tableName(tableName)
				.key(Map.of("PK", AttributeValue.fromS(pk), "SK", AttributeValue.fromS(sk)))).join();
	}

	Map<String, AttributeValue> product(String franchiseId, String branchId, String productId) {
		return client.getItem(request -> request
				.tableName(tableName)
				.consistentRead(true)
				.key(Map.of(
						"PK", AttributeValue.fromS("F#" + franchiseId),
						"SK", AttributeValue.fromS("B#" + branchId + "#P#" + productId))))
				.join()
				.item();
	}

}
