package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.topstock;

import java.util.UUID;

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
 * Recorrido completo por HTTP contra dynamodb-test, con empate, sucursal vacía y cambios de stock.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GetTopStockProductsIT {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void returnsTheTopProductOfEachBranchWithTiesAndWithoutEmptyBranches() {
		String franchiseId = createFranchise();
		BranchResponse centro = createBranch(franchiseId, "Centro");
		BranchResponse norte = createBranch(franchiseId, "Norte");
		createBranch(franchiseId, "Sur");
		addProduct(centro, "Té", 50);
		addProduct(centro, "Café", 50);
		addProduct(centro, "Latte", 10);
		addProduct(norte, "Pan", 30);

		topStock(franchiseId)
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(3)
				.jsonPath("$[0].branchName").isEqualTo("Centro")
				.jsonPath("$[0].productName").isEqualTo("Café")
				.jsonPath("$[1].branchName").isEqualTo("Centro")
				.jsonPath("$[1].productName").isEqualTo("Té")
				.jsonPath("$[2].branchId").isEqualTo(norte.id())
				.jsonPath("$[2].productName").isEqualTo("Pan")
				.jsonPath("$[2].stock").isEqualTo(30);
	}

	@Test
	void reflectsAStockChangeImmediately() {
		String franchiseId = createFranchise();
		BranchResponse centro = createBranch(franchiseId, "Centro");
		addProduct(centro, "Café", 50);
		ProductResponse te = addProduct(centro, "Té", 10);

		webTestClient.patch()
				.uri("/api/franchises/{f}/branches/{b}/products/{p}/stock", franchiseId, centro.id(), te.id())
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"stock\":80}")
				.exchange()
				.expectStatus().isOk();

		topStock(franchiseId)
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.length()").isEqualTo(1)
				.jsonPath("$[0].productName").isEqualTo("Té")
				.jsonPath("$[0].stock").isEqualTo(80);
	}

	@Test
	void unknownFranchiseReturns404() {
		topStock(UUID.randomUUID().toString()).expectStatus().isNotFound();
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['/api/franchises/{franchiseId}/top-stock-products'].get.summary")
				.isEqualTo("Producto con más stock por sucursal");
	}

	private WebTestClient.ResponseSpec topStock(String franchiseId) {
		return webTestClient.get().uri("/api/franchises/{franchiseId}/top-stock-products", franchiseId).exchange();
	}

	private String createFranchise() {
		return post("/api/franchises", "{\"name\":\"Franquicia de prueba\"}", FranchiseResponse.class).id();
	}

	private BranchResponse createBranch(String franchiseId, String name) {
		return post("/api/franchises/" + franchiseId + "/branches", "{\"name\":\"" + name + "\"}", BranchResponse.class);
	}

	private ProductResponse addProduct(BranchResponse branch, String name, int stock) {
		return post("/api/franchises/" + branch.franchiseId() + "/branches/" + branch.id() + "/products",
				"{\"name\":\"" + name + "\",\"stock\":" + stock + "}", ProductResponse.class);
	}

	private <T> T post(String uri, String json, Class<T> type) {
		return webTestClient.post().uri(uri)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(type).returnResult().getResponseBody();
	}

}
