package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.addproduct;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.dto.FranchiseResponse;

/**
 * Recorrido completo por HTTP contra dynamodb-test: franquicia, sucursal y sus productos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AddProductIT {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void addsProductToAnExistingBranch() {
		BranchResponse branch = createBranch();

		addProduct(branch.franchiseId(), branch.id(), "{\"name\":\"Café americano\",\"stock\":25}")
				.expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.franchiseId").isEqualTo(branch.franchiseId())
				.jsonPath("$.branchId").isEqualTo(branch.id())
				.jsonPath("$.name").isEqualTo("Café americano")
				.jsonPath("$.stock").isEqualTo(25);
	}

	@Test
	void duplicateNameInTheSameBranchReturns409() {
		BranchResponse branch = createBranch();
		addProduct(branch.franchiseId(), branch.id(), "{\"name\":\"Café\",\"stock\":1}").expectStatus().isCreated();

		addProduct(branch.franchiseId(), branch.id(), "{\"name\":\"CAFE\",\"stock\":2}")
				.expectStatus().isEqualTo(HttpStatus.CONFLICT)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.title").isEqualTo("Recurso duplicado");
	}

	@Test
	void unknownBranchReturns404() {
		BranchResponse branch = createBranch();
		String missingBranchId = UUID.randomUUID().toString();

		addProduct(branch.franchiseId(), missingBranchId, "{\"name\":\"Café\",\"stock\":1}")
				.expectStatus().isNotFound()
				.expectBody()
				.jsonPath("$.detail").isEqualTo("La sucursal '" + missingBranchId + "' no existe");
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['/api/franchises/{franchiseId}/branches/{branchId}/products'].post.summary")
				.isEqualTo("Agregar un producto a una sucursal");
	}

	private BranchResponse createBranch() {
		String franchiseId = webTestClient.post().uri("/api/franchises")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Franquicia de prueba\"}")
				.exchange()
				.expectBody(FranchiseResponse.class).returnResult().getResponseBody().id();
		return webTestClient.post().uri("/api/franchises/{franchiseId}/branches", franchiseId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Sucursal de prueba\"}")
				.exchange()
				.expectBody(BranchResponse.class).returnResult().getResponseBody();
	}

	private WebTestClient.ResponseSpec addProduct(String franchiseId, String branchId, String json) {
		return webTestClient.post().uri("/api/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branchId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

}
