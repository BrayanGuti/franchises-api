package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.updatestock;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.core.web.error.GlobalExceptionHandler;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.UpdateProductStockUseCase;

import reactor.core.publisher.Mono;

class UpdateProductStockControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String BRANCH_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
	private static final String PRODUCT_ID = "9b2e4c6a-1f3d-4e5b-8a7c-0d1e2f3a4b5c";
	private static final String URI = "/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock";

	private UpdateProductStockUseCase useCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		useCase = mock(UpdateProductStockUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new UpdateProductStockController(useCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void updatesStockAndReturnsTheProduct() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 40))
				.thenReturn(Mono.just(new Product(PRODUCT_ID, FRANCHISE_ID, BRANCH_ID, "Café", 40)));

		patch(PRODUCT_ID, "{\"stock\":40}")
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(PRODUCT_ID)
				.jsonPath("$.name").isEqualTo("Café")
				.jsonPath("$.stock").isEqualTo(40);
	}

	@Test
	void missingProductReturns404() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, 40))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, PRODUCT_ID)));

		patch(PRODUCT_ID, "{\"stock\":40}").expectStatus().isNotFound();
	}

	@Test
	void negativeMissingOrNonNumericStockReturns400WithoutCallingTheUseCase() {
		patch(PRODUCT_ID, "{\"stock\":-1}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.stock").isEqualTo("El stock no puede ser negativo");
		patch(PRODUCT_ID, "{}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.stock").isEqualTo("El stock es obligatorio");
		patch(PRODUCT_ID, "{\"stock\":\"muchos\"}").expectStatus().isBadRequest();

		verify(useCase, never()).execute(anyString(), anyString(), anyString(), anyInt());
	}

	@Test
	void malformedProductIdReturns400() {
		patch("abc", "{\"stock\":1}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.productId").isEqualTo("El identificador debe ser un UUID válido");
	}

	private WebTestClient.ResponseSpec patch(String productId, String json) {
		return client.patch().uri(URI, FRANCHISE_ID, BRANCH_ID, productId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

}
