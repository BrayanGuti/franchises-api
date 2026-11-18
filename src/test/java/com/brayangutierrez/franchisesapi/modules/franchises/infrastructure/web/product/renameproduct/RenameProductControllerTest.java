package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.renameproduct;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.core.web.error.GlobalExceptionHandler;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.RenameProductUseCase;

import reactor.core.publisher.Mono;

class RenameProductControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String BRANCH_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
	private static final String PRODUCT_ID = "9b2e4c6a-1f3d-4e5b-8a7c-0d1e2f3a4b5c";

	private RenameProductUseCase useCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		useCase = mock(RenameProductUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new RenameProductController(useCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void renamesAndReturnsTheProductWithItsStock() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Espresso"))
				.thenReturn(Mono.just(new Product(PRODUCT_ID, FRANCHISE_ID, BRANCH_ID, "Espresso", 7)));

		patch(PRODUCT_ID, "{\"name\":\"Espresso\"}")
				.expectStatus().isOk()
				.expectBody().jsonPath("$.name").isEqualTo("Espresso").jsonPath("$.stock").isEqualTo(7);
	}

	@Test
	void takenNameReturns409() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Té"))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.PRODUCT_ALREADY_EXISTS, "Té")));

		patch(PRODUCT_ID, "{\"name\":\"Té\"}")
				.expectStatus().isEqualTo(HttpStatus.CONFLICT)
				.expectBody().jsonPath("$.detail").isEqualTo("El producto 'Té' ya existe en la sucursal");
	}

	@Test
	void missingProductReturns404() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID, PRODUCT_ID, "Espresso"))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, PRODUCT_ID)));

		patch(PRODUCT_ID, "{\"name\":\"Espresso\"}").expectStatus().isNotFound();
	}

	@Test
	void invalidNameOrIdReturns400() {
		patch(PRODUCT_ID, "{\"name\":\" \"}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio");
		patch("abc", "{\"name\":\"Espresso\"}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.productId").isEqualTo("El identificador debe ser un UUID válido");
	}

	private WebTestClient.ResponseSpec patch(String productId, String json) {
		return client.patch()
				.uri("/api/franchises/{f}/branches/{b}/products/{p}", FRANCHISE_ID, BRANCH_ID, productId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

}
