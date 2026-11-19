package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.gateways.InventoryRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Lectura del inventario contra dynamodb-test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class InventoryDynamoRepositoryIT {

	@Autowired
	private InventoryRepository inventoryRepository;

	@Autowired
	private FranchiseRepository franchiseRepository;

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void readsTheFranchiseWithItsBranchesAndProductsButNotTheNameReservations() {
		Franchise franchise = franchiseRepository.save(new Franchise(uniqueId(), "Cafeterías")).block();
		Branch centro = branchRepository.save(new Branch(uniqueId(), franchise.id(), "Centro")).block();
		Branch sur = branchRepository.save(new Branch(uniqueId(), franchise.id(), "Sur")).block();
		Product cafe = productRepository.save(new Product(uniqueId(), franchise.id(), centro.id(), "Café", 50)).block();
		Product te = productRepository.save(new Product(uniqueId(), franchise.id(), centro.id(), "Té", 7)).block();

		StepVerifier.create(inventoryRepository.findByFranchiseId(franchise.id()))
				.assertNext(inventory -> {
					assertThat(inventory.franchise()).isEqualTo(franchise);
					assertThat(inventory.branches()).containsExactlyInAnyOrder(centro, sur);
					assertThat(inventory.products()).containsExactlyInAnyOrder(cafe, te);
				})
				.verifyComplete();
	}

	@Test
	void seesAWriteImmediatelyAfterItIsConfirmed() {
		Franchise franchise = franchiseRepository.save(new Franchise(uniqueId(), "Cafeterías")).block();
		Branch branch = branchRepository.save(new Branch(uniqueId(), franchise.id(), "Centro")).block();
		Product product = productRepository.save(new Product(uniqueId(), franchise.id(), branch.id(), "Café", 1)).block();
		productRepository.updateStock(franchise.id(), branch.id(), product.id(), 99).block();

		StepVerifier.create(inventoryRepository.findByFranchiseId(franchise.id()))
				.assertNext(inventory -> assertThat(inventory.products())
						.singleElement()
						.satisfies(stored -> assertThat(stored.stock()).isEqualTo(99)))
				.verifyComplete();
	}

	@Test
	void franchiseWithoutBranchesHasAnEmptyInventory() {
		Franchise franchise = franchiseRepository.save(new Franchise(uniqueId(), "Sin sucursales")).block();

		StepVerifier.create(inventoryRepository.findByFranchiseId(franchise.id()))
				.assertNext(inventory -> {
					assertThat(inventory.branches()).isEmpty();
					assertThat(inventory.products()).isEmpty();
				})
				.verifyComplete();
	}

	@Test
	void aBranchBeingDeletedIsLeftOutTogetherWithItsProducts() {
		Franchise franchise = franchiseRepository.save(new Franchise(uniqueId(), "Cafeterías")).block();
		Branch centro = branchRepository.save(new Branch(uniqueId(), franchise.id(), "Centro")).block();
		Branch norte = branchRepository.save(new Branch(uniqueId(), franchise.id(), "Norte")).block();
		productRepository.save(new Product(uniqueId(), franchise.id(), centro.id(), "Café", 50)).block();
		client.updateItem(request -> request
				.tableName(properties.tableName())
				.key(Map.of("PK", AttributeValue.fromS("F#" + franchise.id()),
						"SK", AttributeValue.fromS("B#" + centro.id())))
				.updateExpression("SET #status = :deleting")
				.expressionAttributeNames(Map.of("#status", "status"))
				.expressionAttributeValues(Map.of(":deleting", AttributeValue.fromS("DELETING"))))
				.join();

		StepVerifier.create(inventoryRepository.findByFranchiseId(franchise.id()))
				.assertNext(inventory -> assertThat(inventory.branches()).containsExactly(norte))
				.verifyComplete();
	}

	@Test
	void missingFranchiseIsEmpty() {
		StepVerifier.create(inventoryRepository.findByFranchiseId(uniqueId())).verifyComplete();
	}

	private static String uniqueId() {
		return UUID.randomUUID().toString();
	}

}
