package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductDynamoRepositoryIT {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private FranchiseRepository franchiseRepository;

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void savesProductAndItsNameReservation() {
		Branch branch = existingBranch();
		Product product = product(branch, "Café Americano", 25);

		StepVerifier.create(productRepository.save(product)).expectNext(product).verifyComplete();

		List<Map<String, AttributeValue>> items = itemsOf(branch);
		assertThat(items).extracting(item -> item.get("entityType").s())
				.containsExactlyInAnyOrder("BRANCH", "PRODUCT", "PRODUCT_NAME");
		assertThat(items).anySatisfy(item -> {
			assertThat(item.get("SK").s()).isEqualTo("B#" + branch.id() + "#P#" + product.id());
			assertThat(item.get("stock").n()).isEqualTo("25");
		});
		assertThat(items).anySatisfy(item -> {
			assertThat(item.get("SK").s()).isEqualTo("B#" + branch.id() + "#NAME#cafe americano");
			assertThat(item.get("productId").s()).isEqualTo(product.id());
		});
	}

	@Test
	void failsWhenBranchDoesNotExistAndWritesNothing() {
		Branch missing = new Branch(uniqueId(), existingBranch().franchiseId(), "No existe");

		StepVerifier.create(productRepository.save(product(missing, "Huérfano", 1)))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.BRANCH_NOT_FOUND))
				.verify();

		assertThat(itemsOf(missing)).isEmpty();
	}

	@Test
	void equivalentNameInTheSameBranchIsRejectedAndKeepsASingleProduct() {
		Branch branch = existingBranch();
		productRepository.save(product(branch, "Café Americano", 10)).block();

		StepVerifier.create(productRepository.save(product(branch, "  CAFE americano ", 99)))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_ALREADY_EXISTS))
				.verify();

		assertThat(itemsOf(branch)).filteredOn(item -> "PRODUCT".equals(item.get("entityType").s()))
				.singleElement()
				.satisfies(item -> assertThat(item.get("stock").n()).isEqualTo("10"));
	}

	@Test
	void sameNameIsAllowedInDifferentBranches() {
		Branch first = existingBranch();
		Branch second = existingBranch();

		StepVerifier.create(productRepository.save(product(first, "Café", 1))).expectNextCount(1).verifyComplete();
		StepVerifier.create(productRepository.save(product(second, "Café", 1))).expectNextCount(1).verifyComplete();
	}

	private Branch existingBranch() {
		Franchise franchise = franchiseRepository.save(new Franchise(uniqueId(), "Franquicia de prueba")).block();
		return branchRepository.save(new Branch(uniqueId(), franchise.id(), "Sucursal de prueba")).block();
	}

	private static Product product(Branch branch, String name, int stock) {
		return new Product(uniqueId(), branch.franchiseId(), branch.id(), name, stock);
	}

	/** Todos los ítems cuya sort key empieza por B#{branchId}: la sucursal, sus productos y reservas. */
	private List<Map<String, AttributeValue>> itemsOf(Branch branch) {
		return client.query(request -> request
				.tableName(properties.tableName())
				.consistentRead(true)
				.keyConditionExpression("PK = :pk AND begins_with(SK, :sk)")
				.expressionAttributeValues(Map.of(
						":pk", AttributeValue.fromS("F#" + branch.franchiseId()),
						":sk", AttributeValue.fromS("B#" + branch.id()))))
				.join()
				.items();
	}

	private static String uniqueId() {
		return UUID.randomUUID().toString();
	}

}
