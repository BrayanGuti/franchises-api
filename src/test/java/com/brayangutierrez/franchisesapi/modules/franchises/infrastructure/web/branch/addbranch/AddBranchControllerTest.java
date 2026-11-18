package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.addbranch;

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

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch.AddBranchUseCase;
import com.brayangutierrez.franchisesapi.core.web.error.GlobalExceptionHandler;

import reactor.core.publisher.Mono;

class AddBranchControllerTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";
	private static final String URI = "/api/franchises/{franchiseId}/branches";

	private AddBranchUseCase addBranchUseCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		addBranchUseCase = mock(AddBranchUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new AddBranchController(addBranchUseCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void addsBranchAndReturns201() {
		when(addBranchUseCase.execute(FRANCHISE_ID, "Sucursal Centro"))
				.thenReturn(Mono.just(new Branch("b-1", FRANCHISE_ID, "Sucursal Centro")));

		post(FRANCHISE_ID, "{\"name\":\"Sucursal Centro\"}")
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.id").isEqualTo("b-1")
				.jsonPath("$.franchiseId").isEqualTo(FRANCHISE_ID)
				.jsonPath("$.name").isEqualTo("Sucursal Centro");
	}

	@Test
	void unknownFranchiseReturns404() {
		when(addBranchUseCase.execute(FRANCHISE_ID, "Sucursal Centro"))
				.thenReturn(Mono.error(new BusinessException(FranchiseError.FRANCHISE_NOT_FOUND, FRANCHISE_ID)));

		post(FRANCHISE_ID, "{\"name\":\"Sucursal Centro\"}")
				.expectStatus().isNotFound()
				.expectBody()
				.jsonPath("$.detail").isEqualTo("La franquicia '" + FRANCHISE_ID + "' no existe");
	}

	@Test
	void malformedFranchiseIdReturns400WithoutCallingTheUseCase() {
		post("no-es-un-uuid", "{\"name\":\"Sucursal Centro\"}")
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.franchiseId").isEqualTo("El identificador debe ser un UUID válido");

		verify(addBranchUseCase, never()).execute(anyString(), anyString());
	}

	@Test
	void blankNameReturns400() {
		post(FRANCHISE_ID, "{\"name\":\" \"}")
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio");
	}

	private WebTestClient.ResponseSpec post(String franchiseId, String json) {
		return client.post().uri(URI, franchiseId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

}
