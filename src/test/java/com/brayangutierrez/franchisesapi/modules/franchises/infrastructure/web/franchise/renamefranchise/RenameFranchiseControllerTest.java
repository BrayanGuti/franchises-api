package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.renamefranchise;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.core.web.error.GlobalExceptionHandler;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.franchise.RenameFranchiseUseCase;

import reactor.core.publisher.Mono;

class RenameFranchiseControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";

	private RenameFranchiseUseCase useCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		useCase = mock(RenameFranchiseUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new RenameFranchiseController(useCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void renamesAndReturnsTheFranchise() {
		when(useCase.execute(FRANCHISE_ID, "Nuevo")).thenReturn(Mono.just(new Franchise(FRANCHISE_ID, "Nuevo")));

		patch(FRANCHISE_ID, "{\"name\":\"Nuevo\"}")
				.expectStatus().isOk()
				.expectBody().jsonPath("$.id").isEqualTo(FRANCHISE_ID).jsonPath("$.name").isEqualTo("Nuevo");
	}

	@Test
	void missingFranchiseReturns404() {
		when(useCase.execute(FRANCHISE_ID, "Nuevo"))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.FRANCHISE_NOT_FOUND, FRANCHISE_ID)));

		patch(FRANCHISE_ID, "{\"name\":\"Nuevo\"}").expectStatus().isNotFound();
	}

	@Test
	void invalidNameOrIdReturns400() {
		patch(FRANCHISE_ID, "{\"name\":\"\"}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio");
		patch("abc", "{\"name\":\"Nuevo\"}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.franchiseId").isEqualTo("El identificador debe ser un UUID válido");
	}

	private WebTestClient.ResponseSpec patch(String franchiseId, String json) {
		return client.patch().uri("/api/franchises/{franchiseId}", franchiseId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

}
