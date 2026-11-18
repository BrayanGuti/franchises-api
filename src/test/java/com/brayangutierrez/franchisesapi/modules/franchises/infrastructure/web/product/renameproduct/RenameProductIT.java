package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.renameproduct;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.ApiFixtures;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto.ProductResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RenameProductIT {

	@Autowired
	private WebTestClient webTestClient;

	private ApiFixtures api;

	@BeforeEach
	void setUp() {
		api = new ApiFixtures(webTestClient);
	}

	@Test
	void renamesKeepingTheStockAndFreesTheOldName() {
		BranchResponse branch = api.createBranch();
		ProductResponse product = api.addProduct(branch, "Café", 7);

		api.patch(uri(product), "{\"name\":\"Espresso\"}")
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.name").isEqualTo("Espresso")
				.jsonPath("$.stock").isEqualTo(7);

		api.addProduct(branch, "Café", 1);
	}

	@Test
	void renamingToATakenNameReturns409() {
		BranchResponse branch = api.createBranch();
		ProductResponse cafe = api.addProduct(branch, "Café", 7);
		api.addProduct(branch, "Té", 3);

		api.patch(uri(cafe), "{\"name\":\"TE\"}")
				.expectStatus().isEqualTo(HttpStatus.CONFLICT)
				.expectBody().jsonPath("$.title").isEqualTo("Recurso duplicado");
	}

	@Test
	void renamingADeletedProductReturns404() {
		ProductResponse product = api.addProduct(api.createBranch(), "Café", 7);
		webTestClient.delete().uri(uri(product)).exchange().expectStatus().isNoContent();

		api.patch(uri(product), "{\"name\":\"Espresso\"}").expectStatus().isNotFound();
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs").exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}'].patch.summary")
				.isEqualTo("Actualizar el nombre de un producto");
	}

	private static String uri(ProductResponse product) {
		return "/api/franchises/" + product.franchiseId() + "/branches/" + product.branchId() + "/products/" + product.id();
	}

}
