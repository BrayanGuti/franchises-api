package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Eliminación de sucursales contra dynamodb-test: lápida, barrido e idempotencia.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RemoveBranchOperationIT {

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private FranchiseRepository franchiseRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void deletesTheBranchWithItsProductsAndReservationsWithoutTouchingOtherBranches() {
		String franchiseId = existingFranchise();
		Branch centro = branch(franchiseId, "Centro");
		Branch norte = branch(franchiseId, "Norte");
		addProducts(centro, "Café", "Té", "Latte");
		addProducts(norte, "Pan");

		StepVerifier.create(branchRepository.delete(franchiseId, centro.id())).verifyComplete();

		assertThat(skOf(franchiseId)).noneMatch(sk -> sk.startsWith("B#" + centro.id()));
		assertThat(skOf(franchiseId)).filteredOn(sk -> sk.startsWith("B#" + norte.id())).hasSize(3);
	}

	@Test
	void deletesAnEmptyBranch() {
		String franchiseId = existingFranchise();
		Branch branch = branch(franchiseId, "Vacía");

		StepVerifier.create(branchRepository.delete(franchiseId, branch.id())).verifyComplete();

		assertThat(skOf(franchiseId)).containsExactly("META");
	}

	@Test
	void resumesAnInterruptedDeletion() {
		String franchiseId = existingFranchise();
		Branch branch = branch(franchiseId, "Centro");
		addProducts(branch, "Café", "Té");
		markDeleting(branch);

		StepVerifier.create(branchRepository.delete(franchiseId, branch.id())).verifyComplete();

		assertThat(skOf(franchiseId)).containsExactly("META");
	}

	@Test
	void onceMarkedNoProductCanBeAdded() {
		String franchiseId = existingFranchise();
		Branch branch = branch(franchiseId, "Centro");
		markDeleting(branch);

		StepVerifier.create(productRepository.save(new Product(uniqueId(), franchiseId, branch.id(), "Café", 1)))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.BRANCH_NOT_FOUND))
				.verify();

		assertThat(skOf(franchiseId)).containsExactlyInAnyOrder("META", "B#" + branch.id());
	}

	@Test
	void missingOrAlreadyDeletedBranchFailsWithNotFound() {
		String franchiseId = existingFranchise();
		Branch branch = branch(franchiseId, "Centro");
		branchRepository.delete(franchiseId, branch.id()).block();

		StepVerifier.create(branchRepository.delete(franchiseId, branch.id()))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.BRANCH_NOT_FOUND))
				.verify();
		StepVerifier.create(branchRepository.delete(franchiseId, uniqueId()))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.BRANCH_NOT_FOUND))
				.verify();
	}

	private String existingFranchise() {
		return franchiseRepository.save(new Franchise(uniqueId(), "Franquicia de prueba")).block().id();
	}

	private Branch branch(String franchiseId, String name) {
		return branchRepository.save(new Branch(uniqueId(), franchiseId, name)).block();
	}

	private void addProducts(Branch branch, String... names) {
		for (String name : names) {
			productRepository.save(new Product(uniqueId(), branch.franchiseId(), branch.id(), name, 5)).block();
		}
	}

	/** Simula un borrado que se interrumpió después de marcar la sucursal. */
	private void markDeleting(Branch branch) {
		client.updateItem(request -> request
				.tableName(properties.tableName())
				.key(Map.of("PK", AttributeValue.fromS("F#" + branch.franchiseId()),
						"SK", AttributeValue.fromS("B#" + branch.id())))
				.updateExpression("SET #status = :deleting")
				.expressionAttributeNames(Map.of("#status", "status"))
				.expressionAttributeValues(Map.of(":deleting", AttributeValue.fromS("DELETING"))))
				.join();
	}

	/** Sort keys de todos los ítems de la franquicia. */
	private List<String> skOf(String franchiseId) {
		return client.query(request -> request
				.tableName(properties.tableName())
				.consistentRead(true)
				.keyConditionExpression("PK = :pk")
				.expressionAttributeValues(Map.of(":pk", AttributeValue.fromS("F#" + franchiseId))))
				.join()
				.items().stream().map(item -> item.get("SK").s()).toList();
	}

	private static String uniqueId() {
		return UUID.randomUUID().toString();
	}

}
