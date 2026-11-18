package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import static org.mockito.Mockito.when;

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
class RemoveProductUseCaseTest {

	private static final String FRANCHISE_ID = "f-1";
	private static final String BRANCH_ID = "b-1";
	private static final String PRODUCT_ID = "p-1";

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private RemoveProductUseCase useCase;

	@Test
	void deletesTheProduct() {
		when(productRepository.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.empty());

		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)).verifyComplete();
	}

	@Test
	void propagatesProductNotFound() {
		BusinessException notFound = new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, PRODUCT_ID);
		when(productRepository.delete(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.error(notFound));

		StepVerifier.create(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
				.expectErrorMatches(notFound::equals)
				.verify();
	}

}
