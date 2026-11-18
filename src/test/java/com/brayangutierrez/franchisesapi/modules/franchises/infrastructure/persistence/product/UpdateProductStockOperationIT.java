package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import static com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product.ProductFixtures.product;
import static com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product.ProductFixtures.uniqueId;
import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * Cambio de stock contra dynamodb-test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ProductFixtures.class)
class UpdateProductStockOperationIT {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductFixtures fixtures;

	@Test
	void updatesOnlyTheStockAndReturnsTheUpdatedProduct() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café Americano", 5)).block();

		StepVerifier.create(updateStock(product, 40))
				.expectNext(new Product(product.id(), product.franchiseId(), product.branchId(), "Café Americano", 40))
				.verifyComplete();

		assertThat(fixtures.itemsOf(branch))
				.filteredOn(item -> "PRODUCT".equals(item.get("entityType").s()))
				.singleElement()
				.satisfies(item -> {
					assertThat(item.get("stock").n()).isEqualTo("40");
					assertThat(item.get("name").s()).isEqualTo("Café Americano");
				});
		assertThat(fixtures.entityTypesOf(branch)).containsExactlyInAnyOrder("BRANCH", "PRODUCT", "PRODUCT_NAME");
	}

	@Test
	void stockCanBeSetToZero() {
		Product product = productRepository.save(product(fixtures.existingBranch(), "Café", 5)).block();

		StepVerifier.create(updateStock(product, 0))
				.assertNext(updated -> assertThat(updated.stock()).isZero())
				.verifyComplete();
	}

	@Test
	void missingProductFailsWithNotFoundAndIsNotCreated() {
		Branch branch = fixtures.existingBranch();

		StepVerifier.create(productRepository.updateStock(branch.franchiseId(), branch.id(), uniqueId(), 7))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_NOT_FOUND))
				.verify();

		assertThat(fixtures.entityTypesOf(branch)).containsExactly("BRANCH");
	}

	@Test
	void deletedProductIsNotResurrectedByAStockUpdate() {
		Branch branch = fixtures.existingBranch();
		Product product = productRepository.save(product(branch, "Café", 5)).block();
		productRepository.delete(product.franchiseId(), product.branchId(), product.id()).block();

		StepVerifier.create(updateStock(product, 99))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.PRODUCT_NOT_FOUND))
				.verify();

		assertThat(fixtures.entityTypesOf(branch)).containsExactly("BRANCH");
	}

	private Mono<Product> updateStock(Product product, int stock) {
		return productRepository.updateStock(product.franchiseId(), product.branchId(), product.id(), stock);
	}

}
