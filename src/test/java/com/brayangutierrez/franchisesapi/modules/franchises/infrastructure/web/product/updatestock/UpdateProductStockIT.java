package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.updatestock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.addbranch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.createfranchise.dto.FranchiseResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto.ProductResponse;

/**
 * Recorrido completo por HTTP contra dynamodb-test: cambiar el stock y no resucitar productos borrados.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class UpdateProductStockIT {

	private static final String PRODUCTS = "/api/franchises/{franchiseId}/branches/{branchId}/products";

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void updatesTheStockKeepingTheName() {
		ProductResponse product = createProduct("Café", 5);

		patchStock(product, 40)
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.name").isEqualTo("Café")
				.jsonPath("$.stock").isEqualTo(40);
	}

	@Test
	void patchAfterDeleteReturns404() {
		ProductResponse product = createProduct("Café", 5);
		webTestClient.delete().uri(PRODUCTS + "/{productId}", product.franchiseId(), product.branchId(), product.id())
				.exchange()
				.expectStatus().isNoContent();

		patchStock(product, 40)
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['" + PRODUCTS + "/{productId}/stock'].patch.summary")
				.isEqualTo("Modificar el stock de un producto");
	}

	private WebTestClient.ResponseSpec patchStock(ProductResponse product, int stock) {
		return webTestClient.patch()
				.uri(PRODUCTS + "/{productId}/stock", product.franchiseId(), product.branchId(), product.id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"stock\":" + stock + "}")
				.exchange();
	}

	private ProductResponse createProduct(String name, int stock) {
		String franchiseId = webTestClient.post().uri("/api/franchises")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Franquicia de prueba\"}")
				.exchange()
				.expectBody(FranchiseResponse.class).returnResult().getResponseBody().id();
		BranchResponse branch = webTestClient.post().uri("/api/franchises/{franchiseId}/branches", franchiseId)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"Sucursal de prueba\"}")
				.exchange()
				.expectBody(BranchResponse.class).returnResult().getResponseBody();
		return webTestClient.post().uri(PRODUCTS, franchiseId, branch.id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"name\":\"" + name + "\",\"stock\":" + stock + "}")
				.exchange()
				.expectBody(ProductResponse.class).returnResult().getResponseBody();
	}

}
