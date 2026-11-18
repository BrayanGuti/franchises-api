package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.removeproduct;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.core.web.error.GlobalExceptionHandler;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.RemoveProductUseCase;

import reactor.core.publisher.Mono;

class RemoveProductControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String BRANCH_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
	private static final String PRODUCT_ID = "9b2e4c6a-1f3d-4e5b-8a7c-0d1e2f3a4b5c";
	private static final String URI = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}";

	private RemoveProductUseCase removeProductUseCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		removeProductUseCase = mock(RemoveProductUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new RemoveProductController(removeProductUseCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void deletesProductAndReturns204WithoutBody() {
		when(removeProductUseCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID)).thenReturn(Mono.empty());

		delete(PRODUCT_ID).expectStatus().isNoContent().expectBody().isEmpty();
	}

	@Test
	void missingProductReturns404() {
		when(removeProductUseCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, PRODUCT_ID)));

		delete(PRODUCT_ID)
				.expectStatus().isNotFound()
				.expectBody().jsonPath("$.detail").isEqualTo("El producto '" + PRODUCT_ID + "' no existe");
	}

	@Test
	void concurrentModificationReturns409() {
		when(removeProductUseCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.PRODUCT_MODIFIED_CONCURRENTLY, PRODUCT_ID)));

		delete(PRODUCT_ID)
				.expectStatus().isEqualTo(HttpStatus.CONFLICT)
				.expectBody().jsonPath("$.title").isEqualTo("Conflicto de concurrencia");
	}

	@Test
	void malformedProductIdReturns400WithoutCallingTheUseCase() {
		delete("123")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.productId").isEqualTo("El identificador debe ser un UUID válido");

		verify(removeProductUseCase, never()).execute(anyString(), anyString(), anyString());
	}

	private WebTestClient.ResponseSpec delete(String productId) {
		return client.delete().uri(URI, FRANCHISE_ID, BRANCH_ID, productId).exchange();
	}

}
