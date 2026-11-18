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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Renombrado de productos contra dynamodb-test: la reserva del nombre se mueve de forma atómica.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ProductFixtures.class)
class RenameProductOperationIT {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductFixtures fixtures;

	@Test
	void renamesKeepingTheStockAndMovesTheReservation() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 7)).block();

		StepVerifier.create(rename(product, "Espresso"))
				.expectNext(new Product(product.id(), product.franchiseId(), product.branchId(), "Espresso", 7))
				.verifyComplete();

		assertThat(reservationsOf(branch)).containsExactly("B#" + branch.id() + "#NAME#espresso");
	}

	@Test
	void theOldNameIsFreeAndTheNewOneIsTaken() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 7)).block();
		rename(product, "Espresso").block();

		StepVerifier.create(productRepository.save(product(branch, "CAFE", 1))).expectNextCount(1).verifyComplete();
		StepVerifier.create(productRepository.save(product(branch, "espresso", 1)))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_ALREADY_EXISTS))
				.verify();
	}

	@Test
	void renamingToANameUsedByAnotherProductFailsAndChangesNothing() {
		Branch branch = fixtures.existingBranch();
		Product cafe = productRepository.save(product(branch, "Café", 7)).block();
		productRepository.save(product(branch, "Té", 3)).block();

		StepVerifier.create(rename(cafe, "  TÉ "))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_ALREADY_EXISTS))
				.verify();

		assertThat(namesOf(branch)).containsExactlyInAnyOrder("Café", "Té");
		assertThat(reservationsOf(branch)).hasSize(2);
	}

	@Test
	void changingOnlyCaseOrAccentsKeepsASingleReservation() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "cafe", 7)).block();

		StepVerifier.create(rename(product, "Café"))
				.assertNext(renamed -> assertThat(renamed.name()).isEqualTo("Café"))
				.verifyComplete();

		assertThat(reservationsOf(branch)).containsExactly("B#" + branch.id() + "#NAME#cafe");
	}

	@Test
	void missingProductFailsWithNotFoundAndWritesNothing() {
		Branch branch = fixtures.existingBranch();

		StepVerifier.create(productRepository.rename(branch.franchiseId(), branch.id(), uniqueId(), "Fantasma"))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_NOT_FOUND))
				.verify();

		assertThat(fixtures.entityTypesOf(branch)).containsExactly("BRANCH");
	}

	@Test
	void aProductOfABranchBeingDeletedCannotBeRenamed() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 7)).block();
		fixtures.markBranchDeleting(branch);

		StepVerifier.create(rename(product, "Espresso"))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.BRANCH_NOT_FOUND))
				.verify();

		assertThat(namesOf(branch)).containsExactly("Café");
		assertThat(reservationsOf(branch)).containsExactly("B#" + branch.id() + "#NAME#cafe");
	}

	@Test
	void neverMovesAReservationOwnedByAnotherProduct() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 7)).block();
		fixtures.putRaw(Map.of(
				"PK", AttributeValue.fromS("F#" + branch.franchiseId()),
				"SK", AttributeValue.fromS("B#" + branch.id() + "#NAME#cafe"),
				"entityType", AttributeValue.fromS("PRODUCT_NAME"),
				"productId", AttributeValue.fromS("otro-producto")));

		StepVerifier.create(rename(product, "Espresso"))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_MODIFIED_CONCURRENTLY))
				.verify();

		assertThat(namesOf(branch)).containsExactly("Café");
	}

	private Mono<Product> rename(Product product, String name) {
		return productRepository.rename(product.franchiseId(), product.branchId(), product.id(), name);
	}

	private List<String> namesOf(Branch branch) {
		return itemsOfType(branch, "PRODUCT").stream().map(item -> item.get("name").s()).toList();
	}

	private List<String> reservationsOf(Branch branch) {
		return itemsOfType(branch, "PRODUCT_NAME").stream().map(item -> item.get("SK").s()).toList();
	}

	private List<Map<String, AttributeValue>> itemsOfType(Branch branch, String entityType) {
		return fixtures.itemsOf(branch).stream()
				.filter(item -> entityType.equals(item.get("entityType").s()))
				.toList();
	}

}
