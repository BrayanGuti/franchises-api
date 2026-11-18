package com.brayangutierrez.franchisesapi.core.web.error;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Peticiones inválidas: cuerpo que no cumple {@code @Valid}, parámetros con restricciones,
 * JSON mal formado y tipos incorrectos. Todas responden 400.
 */
class RequestValidationHandlingTest {

	private final WebTestClient client = ErrorTriggerController.client();

	@Test
	void invalidBodyReturns400WithFieldErrors() {
		client.post().uri("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"  \",\"stock\":-5}")
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo(GlobalExceptionHandler.VALIDATION_TITLE)
				.jsonPath("$.detail").isEqualTo(GlobalExceptionHandler.VALIDATION_DETAIL)
				.jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio")
				.jsonPath("$.errors.stock").isEqualTo("El stock no puede ser negativo");
	}

	@Test
	void validBodyIsAccepted() {
		client.post().uri("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Café\",\"stock\":3}")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	void invalidPathVariableReturns400WithParameterErrors() {
		client.get().uri("/test/items/ID_INVALIDO!").exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.title").isEqualTo(GlobalExceptionHandler.VALIDATION_TITLE)
				.jsonPath("$.errors.id").isEqualTo("El identificador no es válido");
	}

	@Test
	void invalidBodyNextToAValidPathVariableIsReportedByField() {
		client.post().uri("/test/items/abc-1")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"\",\"stock\":3}")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio")
				.jsonPath("$.errors.payload").doesNotExist();
	}

	@Test
	void invalidPathVariableAndBodyAreReportedTogether() {
		client.post().uri("/test/items/ID_INVALIDO!")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"\",\"stock\":-1}")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.id").isEqualTo("El identificador no es válido")
				.jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio")
				.jsonPath("$.errors.stock").isEqualTo("El stock no puede ser negativo");
	}

	@Test
	void malformedJsonReturns400() {
		client.post().uri("/test/body")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{ esto no es json")
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.detail").isEqualTo(GlobalExceptionHandler.MALFORMED_DETAIL);
	}

	@Test
	void typeMismatchReturns400() {
		client.get().uri("/test/numbers/abc").exchange()
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.detail").isEqualTo(GlobalExceptionHandler.MALFORMED_DETAIL);
	}

}
