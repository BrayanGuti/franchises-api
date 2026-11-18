package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.test.context.TestComponent;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;

import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Datos y lecturas crudas de DynamoDB para las pruebas de integración de productos.
 * Cada llamada usa IDs nuevos, así que las pruebas no dependen entre sí.
 */
@TestComponent
class ProductFixtures {

	private final FranchiseRepository franchiseRepository;
	private final BranchRepository branchRepository;
	private final DynamoDbAsyncClient client;
	private final String tableName;

	ProductFixtures(FranchiseRepository franchiseRepository, BranchRepository branchRepository,
			DynamoDbAsyncClient client, DynamoDbProperties properties) {
		this.franchiseRepository = franchiseRepository;
		this.branchRepository = branchRepository;
		this.client = client;
		this.tableName = properties.tableName();
	}

	Branch existingBranch() {
		Franchise franchise = franchiseRepository.save(new Franchise(uniqueId(), "Franquicia de prueba")).block();
		return branchRepository.save(new Branch(uniqueId(), franchise.id(), "Sucursal de prueba")).block();
	}

	static Product product(Branch branch, String name, int stock) {
		return new Product(uniqueId(), branch.franchiseId(), branch.id(), name, stock);
	}

	/** Todos los ítems cuya sort key empieza por B#{branchId}: la sucursal, sus productos y reservas. */
	List<Map<String, AttributeValue>> itemsOf(Branch branch) {
		return client.query(request -> request
				.tableName(tableName)
				.consistentRead(true)
				.keyConditionExpression("PK = :pk AND begins_with(SK, :sk)")
				.expressionAttributeValues(Map.of(
						":pk", AttributeValue.fromS("F#" + branch.franchiseId()),
						":sk", AttributeValue.fromS("B#" + branch.id()))))
				.join()
				.items();
	}

	List<String> entityTypesOf(Branch branch) {
		return itemsOf(branch).stream().map(item -> item.get("entityType").s()).toList();
	}

	void putRaw(Map<String, AttributeValue> item) {
		client.putItem(request -> request.tableName(tableName).item(item)).join();
	}

	void deleteRaw(String pk, String sk) {
		client.deleteItem(request -> request.tableName(tableName)
				.key(Map.of("PK", AttributeValue.fromS(pk), "SK", AttributeValue.fromS(sk)))).join();
	}

	/** Simula la marca que deja el borrado de una sucursal (status = DELETING). */
	void markBranchDeleting(Branch branch) {
		client.updateItem(request -> request
				.tableName(tableName)
				.key(Map.of("PK", AttributeValue.fromS("F#" + branch.franchiseId()),
						"SK", AttributeValue.fromS("B#" + branch.id())))
				.updateExpression("SET #status = :deleting")
				.expressionAttributeNames(Map.of("#status", "status"))
				.expressionAttributeValues(Map.of(":deleting", AttributeValue.fromS("DELETING"))))
				.join();
	}

	static String uniqueId() {
		return UUID.randomUUID().toString();
	}

}
