package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AddProductUseCaseTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String BRANCH_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private AddProductUseCase useCase;

	@Test
	void savesProductInTheBranchWithGeneratedId() {
		when(productRepository.save(any())).thenAnswer(call -> Mono.just(call.getArgument(0)));

		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, " Café americano ", 25))
				.assertNext(product -> {
					assertThat(product.franchiseId()).isEqualTo(FRANCHISE_ID);
					assertThat(product.branchId()).isEqualTo(BRANCH_ID);
					assertThat(product.name()).isEqualTo("Café americano");
					assertThat(product.stock()).isEqualTo(25);
					assertThat(UUID.fromString(product.id())).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void acceptsZeroStock() {
		when(productRepository.save(any())).thenAnswer(call -> Mono.just(call.getArgument(0)));

		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "Agotado", 0))
				.assertNext(product -> assertThat(product.stock()).isZero())
				.verifyComplete();
	}

	@Test
	void rejectsNegativeStockWithoutTouchingTheRepository() {
		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "Café", -1))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.INVALID_STOCK))
				.verify();

		verify(productRepository, never()).save(any());
	}

	@Test
	void rejectsBlankNameWithoutTouchingTheRepository() {
		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "  ", 3))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.INVALID_NAME))
				.verify();

		verify(productRepository, never()).save(any());
	}

	@Test
	void propagatesDuplicateProduct() {
		BusinessException duplicate = new BusinessException(FranchiseError.PRODUCT_ALREADY_EXISTS, "Café");
		when(productRepository.save(any())).thenReturn(Mono.error(duplicate));

		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, "Café", 3))
				.expectErrorMatches(duplicate::equals)
				.verify();
	}

}
