package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import static com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product.ProductFixtures.product;
import static com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product.ProductFixtures.uniqueId;
import static org.assertj.core.api.Assertions.assertThat;

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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Borrado de productos contra dynamodb-test, incluidos los casos de datos inconsistentes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ProductFixtures.class)
class RemoveProductOperationIT {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductFixtures fixtures;

	@Test
	void deletesProductTogetherWithItsNameReservation() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 5)).block();

		StepVerifier.create(delete(product)).verifyComplete();

		assertThat(fixtures.entityTypesOf(branch)).containsExactly("BRANCH");
	}

	@Test
	void deletedNameCanBeUsedAgain() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 5)).block();
		delete(product).block();

		StepVerifier.create(productRepository.save(product(branch, "CAFE", 1))).expectNextCount(1).verifyComplete();
	}

	@Test
	void missingProductFailsWithNotFound() {
		Branch branch = fixtures.existingBranch();

		StepVerifier.create(productRepository.delete(branch.franchiseId(), branch.id(), uniqueId()))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_NOT_FOUND))
				.verify();
	}

	@Test
	void deletingTwiceFailsTheSecondTime() {
		Product product = productRepository.save(product(fixtures.existingBranch(), "Café", 5)).block();
		delete(product).block();

		StepVerifier.create(delete(product))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_NOT_FOUND))
				.verify();
	}

	@Test
	void deletesTheProductEvenIfItsReservationIsMissing() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 5)).block();
		fixtures.deleteRaw("F#" + branch.franchiseId(), "B#" + branch.id() + "#NAME#cafe");

		StepVerifier.create(delete(product)).verifyComplete();

		assertThat(fixtures.entityTypesOf(branch)).containsExactly("BRANCH");
	}

	@Test
	void neverReleasesAReservationOwnedByAnotherProduct() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 5)).block();
		fixtures.putRaw(Map.of(
				"PK", AttributeValue.fromS("F#" + branch.franchiseId()),
				"SK", AttributeValue.fromS("B#" + branch.id() + "#NAME#cafe"),
				"entityType", AttributeValue.fromS("PRODUCT_NAME"),
				"productId", AttributeValue.fromS("otro-producto")));

		StepVerifier.create(delete(product))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_MODIFIED_CONCURRENTLY))
				.verify();

		assertThat(fixtures.entityTypesOf(branch)).containsExactlyInAnyOrder("BRANCH", "PRODUCT", "PRODUCT_NAME");
	}

	private Mono<Void> delete(Product product) {
		return productRepository.delete(product.franchiseId(), product.branchId(), product.id());
	}

}
