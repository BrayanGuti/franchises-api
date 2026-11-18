package com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.branch.addbranch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.branch.addbranch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.franchise.createfranchise.dto.FranchiseResponse;

/**
 * Recorrido completo por HTTP contra dynamodb-test: crear la franquicia y agregarle sucursales.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AddBranchIT {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void addsBranchToAnExistingFranchise() {
		String franchiseId = createFranchise();

		BranchResponse branch = addBranch(franchiseId, "{\"name\":\"Sucursal Centro\"}")
				.expectStatus().isCreated()
				.expectBody(BranchResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(branch).isNotNull();
		assertThat(branch.franchiseId()).isEqualTo(franchiseId);
		assertThat(branch.name()).isEqualTo("Sucursal Centro");
	}

	@Test
	void unknownFranchiseReturns404() {
		String missingId = UUID.randomUUID().toString();

		addBranch(missingId, "{\"name\":\"Sucursal Centro\"}")
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo("Recurso no encontrado")
				.jsonPath("$.detail").isEqualTo("La franquicia '" + missingId + "' no existe");
	}

	@Test
	void malformedFranchiseIdReturns400() {
		addBranch("123", "{\"name\":\"Sucursal Centro\"}")
				.expectStatus().isBadRequest()
				.expectBody()
				.jsonPath("$.errors.franchiseId").isEqualTo("El identificador debe ser un UUID válido");
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['/api/franchises/{franchiseId}/branches'].post.summary")
				.isEqualTo("Agregar una sucursal a una franquicia");
	}

	private String createFranchise() {
		return webTestClient.post().uri("/api/franchises")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Franquicia de prueba\"}")
				.exchange()
				.expectStatus().isCreated()
				.expectBody(FranchiseResponse.class)
				.returnResult()
				.getResponseBody()
				.id();
	}

	private WebTestClient.ResponseSpec addBranch(String franchiseId, String json) {
		return webTestClient.post().uri("/api/franchises/{franchiseId}/branches", franchiseId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

}
