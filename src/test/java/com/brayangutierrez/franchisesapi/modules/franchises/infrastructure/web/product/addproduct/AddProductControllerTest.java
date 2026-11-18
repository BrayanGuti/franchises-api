package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.addproduct;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.AddProductUseCase;

import reactor.core.publisher.Mono;

class AddProductControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String BRANCH_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";
	private static final String URI = "/api/franchises/{franchiseId}/branches/{branchId}/products";
	private static final String VALID_BODY = "{\"name\":\"Café\",\"stock\":5}";

	private AddProductUseCase addProductUseCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		addProductUseCase = mock(AddProductUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new AddProductController(addProductUseCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void addsProductAndReturns201() {
		when(addProductUseCase.execute(FRANCHISE_ID, BRANCH_ID, "Café", 5))
				.thenReturn(Mono.just(new Product("p-1", FRANCHISE_ID, BRANCH_ID, "Café", 5)));

		post(BRANCH_ID, VALID_BODY)
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.id").isEqualTo("p-1")
				.jsonPath("$.branchId").isEqualTo(BRANCH_ID)
				.jsonPath("$.name").isEqualTo("Café")
				.jsonPath("$.stock").isEqualTo(5);
	}

	@Test
	void unknownBranchReturns404() {
		when(addProductUseCase.execute(FRANCHISE_ID, BRANCH_ID, "Café", 5))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.BRANCH_NOT_FOUND, BRANCH_ID)));

		post(BRANCH_ID, VALID_BODY).expectStatus().isNotFound();
	}

	@Test
	void duplicateNameReturns409() {
		when(addProductUseCase.execute(FRANCHISE_ID, BRANCH_ID, "Café", 5))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.PRODUCT_ALREADY_EXISTS, "Café")));

		post(BRANCH_ID, VALID_BODY)
				.expectStatus().isEqualTo(HttpStatus.CONFLICT)
				.expectBody()
				.jsonPath("$.detail").isEqualTo("El producto 'Café' ya existe en la sucursal");
	}

	@Test
	void negativeOrMissingStockReturns400WithoutCallingTheUseCase() {
		post(BRANCH_ID, "{\"name\":\"Café\",\"stock\":-1}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.stock").isEqualTo("El stock no puede ser negativo");
		post(BRANCH_ID, "{\"name\":\"Café\"}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.stock").isEqualTo("El stock es obligatorio");

		verify(addProductUseCase, never()).execute(anyString(), anyString(), anyString(), anyInt());
	}

	@Test
	void malformedBranchIdReturns400() {
		post("no-es-un-uuid", VALID_BODY)
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.branchId").isEqualTo("El identificador debe ser un UUID válido");
	}

	private WebTestClient.ResponseSpec post(String branchId, String json) {
		return client.post().uri(URI, FRANCHISE_ID, branchId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

}
