package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UpdateProductStockUseCaseTest {

	private static final String FRANCHISE_ID = "f-1";
	private static final String BRANCH_ID = "b-1";
	private static final String PRODUCT_ID = "p-1";

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private UpdateProductStockUseCase useCase;

	@Test
	void updatesTheStock() {
		Product updated = new Product(PRODUCT_ID, FRANCHISE_ID, BRANCH_ID, "Café", 40);
		when(productRepository.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 40)).thenReturn(Mono.just(updated));

		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 40))
				.expectNext(updated)
				.verifyComplete();
	}

	@Test
	void rejectsNegativeStockWithoutTouchingTheRepository() {
		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, -3))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.INVALID_STOCK))
				.verify();

		verify(productRepository, never()).updateStock(anyString(), anyString(), anyString(), anyInt());
	}

	@Test
	void propagatesProductNotFound() {
		BusinessException notFound = new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, PRODUCT_ID);
		when(productRepository.updateStock(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 1)).thenReturn(Mono.error(notFound));

		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 1))
				.expectErrorMatches(notFound::equals)
				.verify();
	}

}
