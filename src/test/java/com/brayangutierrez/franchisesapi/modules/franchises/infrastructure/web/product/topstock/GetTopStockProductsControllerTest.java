package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.topstock;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.core.web.error.GlobalExceptionHandler;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.TopStockProduct;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.GetTopStockProductsUseCase;

import reactor.core.publisher.Flux;

class GetTopStockProductsControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String URI = "/api/franchises/{franchiseId}/top-stock-products";

	private GetTopStockProductsUseCase useCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		useCase = mock(GetTopStockProductsUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new GetTopStockProductsController(useCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void returnsTheTopProductsWithTheirBranch() {
		Branch centro = new Branch("b-1", FRANCHISE_ID, "Centro");
		when(useCase.execute(FRANCHISE_ID)).thenReturn(Flux.just(
				new TopStockProduct(centro, new Product("p-1", FRANCHISE_ID, "b-1", "Café", 50)),
				new TopStockProduct(centro, new Product("p-2", FRANCHISE_ID, "b-1", "Té", 50))));

		client.get().uri(URI, FRANCHISE_ID).exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(2)
				.jsonPath("$[0].branchId").isEqualTo("b-1")
				.jsonPath("$[0].branchName").isEqualTo("Centro")
				.jsonPath("$[0].productName").isEqualTo("Café")
				.jsonPath("$[0].stock").isEqualTo(50)
				.jsonPath("$[1].productName").isEqualTo("Té");
	}

	@Test
	void franchiseWithoutProductsReturnsAnEmptyArray() {
		when(useCase.execute(FRANCHISE_ID)).thenReturn(Flux.empty());

		client.get().uri(URI, FRANCHISE_ID).exchange()
				.expectStatus().isOk()
				.expectBody().json("[]");
	}

	@Test
	void missingFranchiseReturns404() {
		when(useCase.execute(FRANCHISE_ID))
				.thenReturn(Flux.error(new BusinessException(FranchiseError.FRANCHISE_NOT_FOUND, FRANCHISE_ID)));

		client.get().uri(URI, FRANCHISE_ID).exchange().expectStatus().isNotFound();
	}

	@Test
	void malformedFranchiseIdReturns400WithoutCallingTheUseCase() {
		client.get().uri(URI, "abc").exchange()
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.franchiseId").isEqualTo("El identificador debe ser un UUID válido");

		verify(useCase, never()).execute(anyString());
	}

}
