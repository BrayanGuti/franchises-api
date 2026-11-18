package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.renamefranchise;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.ApiFixtures;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.dto.FranchiseResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RenameFranchiseIT {

	@Autowired
	private WebTestClient webTestClient;

	private ApiFixtures api;

	@BeforeEach
	void setUp() {
		api = new ApiFixtures(webTestClient);
	}

	@Test
	void renamesAnExistingFranchise() {
		FranchiseResponse franchise = api.createFranchise("Original");

		api.patch("/api/franchises/" + franchise.id(), "{\"name\":\"  Renombrada \"}")
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(franchise.id())
				.jsonPath("$.name").isEqualTo("Renombrada");
	}

	@Test
	void unknownFranchiseReturns404() {
		api.patch("/api/franchises/" + UUID.randomUUID(), "{\"name\":\"Fantasma\"}").expectStatus().isNotFound();
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs").exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['/api/franchises/{franchiseId}'].patch.summary")
				.isEqualTo("Actualizar el nombre de una franquicia");
	}

}
