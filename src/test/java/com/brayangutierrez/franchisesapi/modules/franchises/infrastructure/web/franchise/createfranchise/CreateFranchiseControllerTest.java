package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.createfranchise;

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

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.franchise.CreateFranchiseUseCase;
import com.brayangutierrez.franchisesapi.core.web.error.GlobalExceptionHandler;

import reactor.core.publisher.Mono;

/**
 * Prueba el controlador aislado: el caso de uso es un mock, así que solo se verifica
 * el contrato HTTP (códigos, JSON y validación del DTO).
 */
class CreateFranchiseControllerTest {

	private CreateFranchiseUseCase createFranchiseUseCase;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		createFranchiseUseCase = mock(CreateFranchiseUseCase.class);
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		client = WebTestClient.bindToController(new CreateFranchiseController(createFranchiseUseCase))
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@Test
	void createsFranchiseAndReturns201() {
		when(createFranchiseUseCase.execute("Cafeterías del Norte"))
				.thenReturn(Mono.just(new Franchise("f-1", "Cafeterías del Norte")));

		client.post().uri("/api/franchises")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Cafeterías del Norte\"}")
				.exchange()
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.id").isEqualTo("f-1")
				.jsonPath("$.name").isEqualTo("Cafeterías del Norte");
	}

	@Test
	void rejectsBlankName() {
		postName("{\"name\":\"   \"}")
				.jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio");
		verify(createFranchiseUseCase, never()).execute(anyString());
	}

	@Test
	void rejectsMissingName() {
		postName("{}")
				.jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio");
	}

	@Test
	void rejectsNameLongerThan100Characters() {
		postName("{\"name\":\"" + "a".repeat(101) + "\"}")
				.jsonPath("$.errors.name").isEqualTo("El nombre no puede superar los 100 caracteres");
	}

	private WebTestClient.BodyContentSpec postName(String json) {
		return client.post().uri("/api/franchises")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody();
	}

}
