package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.removeproduct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.dto.FranchiseResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto.ProductResponse;

/**
 * Recorrido completo por HTTP contra dynamodb-test: crear un producto, eliminarlo y reutilizar su nombre.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RemoveProductIT {

	private static final String PRODUCTS = "/api/franchises/{franchiseId}/branches/{branchId}/products";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void deletedProductIsGoneAndItsNameIsFree() {
		BranchResponse branch = createBranch();
		ProductResponse product = addProduct(branch, "Café");

		removeProduct(product).expectStatus().isNoContent();
		removeProduct(product).expectStatus().isNotFound();

		webTestClient.post().uri(PRODUCTS, branch.franchiseId(), branch.id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Café\",\"stock\":1}")
				.exchange()
				.expectStatus().isCreated();
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['" + PRODUCTS + "/{productId}'].delete.summary")
				.isEqualTo("Eliminar un producto de una sucursal");
	}

	private WebTestClient.ResponseSpec removeProduct(ProductResponse product) {
		return webTestClient.delete().uri(PRODUCTS + "/{productId}", product.franchiseId(), product.branchId(), product.id())
				.exchange();
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

	private ProductResponse addProduct(BranchResponse branch, String name) {
		return webTestClient.post().uri(PRODUCTS, branch.franchiseId(), branch.id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"" + name + "\",\"stock\":5}")
				.exchange()
				.expectBody(ProductResponse.class).returnResult().getResponseBody();
	}

}
