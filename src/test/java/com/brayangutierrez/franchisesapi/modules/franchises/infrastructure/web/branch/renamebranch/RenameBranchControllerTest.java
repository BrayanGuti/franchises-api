package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.renamebranch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.core.web.error.GlobalExceptionHandler;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch.RenameBranchUseCase;

import reactor.core.publisher.Mono;

class RenameBranchControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String BRANCH_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

	private RenameBranchUseCase useCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		useCase = mock(RenameBranchUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new RenameBranchController(useCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void renamesAndReturnsTheBranch() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID, "Norte"))
				.thenReturn(Mono.just(new Branch(BRANCH_ID, FRANCHISE_ID, "Norte")));

		patch(BRANCH_ID, "{\"name\":\"Norte\"}")
				.expectStatus().isOk()
				.expectBody().jsonPath("$.id").isEqualTo(BRANCH_ID).jsonPath("$.name").isEqualTo("Norte");
	}

	@Test
	void missingBranchReturns404() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID, "Norte"))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.BRANCH_NOT_FOUND, BRANCH_ID)));

		patch(BRANCH_ID, "{\"name\":\"Norte\"}").expectStatus().isNotFound();
	}

	@Test
	void invalidNameOrIdReturns400() {
		patch(BRANCH_ID, "{}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio");
		patch("abc", "{\"name\":\"Norte\"}")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.branchId").isEqualTo("El identificador debe ser un UUID válido");
	}

	private WebTestClient.ResponseSpec patch(String branchId, String json) {
		return client.patch().uri("/api/franchises/{franchiseId}/branches/{branchId}", FRANCHISE_ID, branchId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

}
