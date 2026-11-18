package com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.franchise.createfranchise;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.franchise.createfranchise.dto.FranchiseResponse;

import reactor.test.StepVerifier;

/**
 * Recorrido completo: HTTP, caso de uso, adaptador y dynamodb-test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class CreateFranchiseIT {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private FranchiseRepository franchiseRepository;

	@Test
	void createdFranchiseIsPersisted() {
		FranchiseResponse created = webTestClient.post().uri("/api/franchises")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"  Cafeterías del Norte  \"}")
				.exchange()
				.expectStatus().isCreated()
				.expectBody(FranchiseResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(created).isNotNull();
		assertThat(created.name()).isEqualTo("Cafeterías del Norte");
		StepVerifier.create(franchiseRepository.findById(created.id()))
				.assertNext(stored -> assertThat(stored.name()).isEqualTo("Cafeterías del Norte"))
				.verifyComplete();
	}

	@Test
	void invalidRequestReturnsProblemDetail() {
		webTestClient.post().uri("/api/franchises")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"\"}")
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(400)
				.jsonPath("$.instance").isEqualTo("/api/franchises")
				.jsonPath("$.errors.name").isEqualTo("El nombre es obligatorio");
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['/api/franchises'].post.summary").isEqualTo("Agregar una franquicia");
	}

}
