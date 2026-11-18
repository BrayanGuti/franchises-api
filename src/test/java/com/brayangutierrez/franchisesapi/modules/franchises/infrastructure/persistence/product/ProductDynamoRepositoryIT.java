package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import static com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product.ProductFixtures.product;
import static com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product.ProductFixtures.uniqueId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Alta de productos contra dynamodb-test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ProductFixtures.class)
class ProductDynamoRepositoryIT {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductFixtures fixtures;

	@Test
	void savesProductAndItsNameReservation() {
		Branch branch = fixtures.existingBranch();
		Product product = product(branch, "Café Americano", 25);

		StepVerifier.create(productRepository.save(product)).expectNext(product).verifyComplete();

		List<Map<String, AttributeValue>> items = fixtures.itemsOf(branch);
		assertThat(fixtures.entityTypesOf(branch)).containsExactlyInAnyOrder("BRANCH", "PRODUCT", "PRODUCT_NAME");
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
		Branch missing = new Branch(uniqueId(), fixtures.existingBranch().franchiseId(), "No existe");

		StepVerifier.create(productRepository.save(product(missing, "Huérfano", 1)))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.BRANCH_NOT_FOUND))
				.verify();

		assertThat(fixtures.itemsOf(missing)).isEmpty();
	}

	@Test
	void equivalentNameInTheSameBranchIsRejectedAndKeepsASingleProduct() {
		Branch branch = fixtures.existingBranch();
		productRepository.save(product(branch, "Café Americano", 10)).block();

		StepVerifier.create(productRepository.save(product(branch, "  CAFE americano ", 99)))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_ALREADY_EXISTS))
				.verify();

		assertThat(fixtures.itemsOf(branch)).filteredOn(item -> "PRODUCT".equals(item.get("entityType").s()))
				.singleElement()
				.satisfies(item -> assertThat(item.get("stock").n()).isEqualTo("10"));
	}

	@Test
	void sameNameIsAllowedInDifferentBranches() {
		Branch first = fixtures.existingBranch();
		Branch second = fixtures.existingBranch();

		StepVerifier.create(productRepository.save(product(first, "Café", 1))).expectNextCount(1).verifyComplete();
		StepVerifier.create(productRepository.save(product(second, "Café", 1))).expectNextCount(1).verifyComplete();
	}

}
