package com.brayangutierrez.franchisesapi.core.web.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Errores de negocio ({@code BusinessException}): la categoría del error define el código HTTP.
 */
class BusinessErrorHandlingTest {

	private final WebTestClient client = ErrorTriggerController.client();

	@Test
	void notFoundReturns404ProblemDetail() {
		client.get().uri("/test/not-found").exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(404)
				.jsonPath("$.title").isEqualTo(GlobalExceptionHandler.NOT_FOUND_TITLE)
				.jsonPath("$.detail").isEqualTo("La franquicia 'f-1' no existe")
				.jsonPath("$.instance").isEqualTo("/test/not-found")
				.jsonPath("$.timestamp").isNotEmpty();
	}

	@Test
	void alreadyExistsReturns409ProblemDetail() {
		client.get().uri("/test/conflict").exchange()
				.expectStatus().isEqualTo(HttpStatus.CONFLICT)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(409)
				.jsonPath("$.title").isEqualTo("Recurso duplicado")
				.jsonPath("$.detail").isEqualTo("El producto 'p-1' ya existe en la sucursal");
	}

	@Test
	void invalidBusinessRuleReturns400ProblemDetail() {
		client.get().uri("/test/invalid").exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(400)
				.jsonPath("$.title").isEqualTo(GlobalExceptionHandler.VALIDATION_TITLE)
				.jsonPath("$.detail").isEqualTo("El stock no puede ser negativo: -1");
	}

}
