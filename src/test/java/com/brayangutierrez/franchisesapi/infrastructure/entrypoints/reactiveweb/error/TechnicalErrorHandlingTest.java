package com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Errores técnicos o de framework: rutas inexistentes, {@code ResponseStatusException} y
 * errores inesperados, que nunca deben exponer detalles internos.
 */
class TechnicalErrorHandlingTest {

	private final WebTestClient client = ErrorTriggerController.client();

	@Test
	void unknownRouteReturns404WithoutInternalDetails() {
		client.get().uri("/test/unknown-route").exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo(GlobalExceptionHandler.NOT_FOUND_TITLE)
				.jsonPath("$.detail").isEqualTo(GlobalExceptionHandler.UNKNOWN_ROUTE_DETAIL);
	}

	@Test
	void responseStatusExceptionKeepsItsStatus() {
		client.get().uri("/test/forbidden").exchange()
				.expectStatus().isForbidden()
				.expectBody()
				.jsonPath("$.status").isEqualTo(403)
				.jsonPath("$.title").isEqualTo("Forbidden")
				.jsonPath("$.detail").isEqualTo("Acceso denegado");
	}

	@Test
	void responseStatusExceptionWithNonStandardCode() {
		client.get().uri("/test/custom-status").exchange()
				.expectStatus().isEqualTo(499)
				.expectBody()
				.jsonPath("$.title").isEqualTo("Error");
	}

	@Test
	void unexpectedErrorReturns500WithoutInternalDetails() {
		client.get().uri("/test/boom").exchange()
				.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
				.expectBody()
				.jsonPath("$.title").isEqualTo("Error interno")
				.jsonPath("$.detail").isEqualTo(GlobalExceptionHandler.INTERNAL_DETAIL);
	}

}
