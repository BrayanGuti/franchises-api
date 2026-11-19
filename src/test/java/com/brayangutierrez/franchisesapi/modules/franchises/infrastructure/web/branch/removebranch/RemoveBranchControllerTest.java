package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.removebranch;

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
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch.RemoveBranchUseCase;

import reactor.core.publisher.Mono;

class RemoveBranchControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String BRANCH_ID = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

	private RemoveBranchUseCase useCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		useCase = mock(RemoveBranchUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new RemoveBranchController(useCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void deletesAndReturns204WithoutBody() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID)).thenReturn(Mono.empty());

		delete(BRANCH_ID).expectStatus().isNoContent().expectBody().isEmpty();
	}

	@Test
	void missingBranchReturns404() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.BRANCH_NOT_FOUND, BRANCH_ID)));

		delete(BRANCH_ID).expectStatus().isNotFound();
	}

	@Test
	void incompleteDeletionReturns409() {
		when(useCase.execute(FRANCHISE_ID, BRANCH_ID))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.BRANCH_DELETION_INCOMPLETE, BRANCH_ID)));

		delete(BRANCH_ID)
				.expectStatus().isEqualTo(HttpStatus.CONFLICT)
				.expectBody().jsonPath("$.title").isEqualTo("Conflicto de concurrencia");
	}

	@Test
	void malformedBranchIdReturns400WithoutCallingTheUseCase() {
		delete("abc")
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors.branchId").isEqualTo("El identificador debe ser un UUID válido");

		verify(useCase, never()).execute(anyString(), anyString());
	}

	private WebTestClient.ResponseSpec delete(String branchId) {
		return client.delete().uri("/api/franchises/{franchiseId}/branches/{branchId}", FRANCHISE_ID, branchId)
				.exchange();
	}

}
