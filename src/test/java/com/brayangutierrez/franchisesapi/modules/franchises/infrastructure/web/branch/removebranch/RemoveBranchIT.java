package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.removebranch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.ApiFixtures;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;

/**
 * Recorrido completo por HTTP contra dynamodb-test: eliminar una sucursal y que deje de existir
 * para todos los endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RemoveBranchIT {

	@Autowired
	private WebTestClient webTestClient;

	private ApiFixtures api;

	@BeforeEach
	void setUp() {
		api = new ApiFixtures(webTestClient);
	}

	@Test
	void deletedBranchDisappearsWithItsProducts() {
		String franchiseId = api.createFranchise("Cafeterías").id();
		BranchResponse centro = api.createBranch(franchiseId, "Centro");
		BranchResponse norte = api.createBranch(franchiseId, "Norte");
		api.addProduct(centro, "Café", 50);
		api.addProduct(norte, "Pan", 30);

		delete(centro).expectStatus().isNoContent();

		webTestClient.get().uri("/api/franchises/{f}/top-stock-products", franchiseId).exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(1)
				.jsonPath("$[0].branchName").isEqualTo("Norte");
	}

	@Test
	void afterDeletionTheBranchNoLongerAcceptsChanges() {
		BranchResponse branch = api.createBranch();
		delete(branch).expectStatus().isNoContent();

		delete(branch).expectStatus().isNotFound();
		webTestClient.post().uri("/api/franchises/{f}/branches/{b}/products", branch.franchiseId(), branch.id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Café\",\"stock\":1}")
				.exchange()
				.expectStatus().isNotFound();
		api.patch("/api/franchises/" + branch.franchiseId() + "/branches/" + branch.id(), "{\"name\":\"Norte\"}")
				.expectStatus().isNotFound();
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs").exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['/api/franchises/{franchiseId}/branches/{branchId}'].delete.summary")
				.isEqualTo("Eliminar una sucursal y sus productos");
	}

	private WebTestClient.ResponseSpec delete(BranchResponse branch) {
		return webTestClient.delete().uri("/api/franchises/{f}/branches/{b}", branch.franchiseId(), branch.id())
				.exchange();
	}

}
