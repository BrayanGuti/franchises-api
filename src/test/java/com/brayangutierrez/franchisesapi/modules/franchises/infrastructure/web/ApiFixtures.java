package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web;

import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.dto.FranchiseResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto.ProductResponse;

/**
 * Crea datos de prueba a través de la API real (HTTP), para las pruebas de integración web.
 */
public final class ApiFixtures {

	private final WebTestClient client;

	public ApiFixtures(WebTestClient client) {
		this.client = client;
	}

	public FranchiseResponse createFranchise(String name) {
		return post("/api/franchises", "{\"name\":\"" + name + "\"}", FranchiseResponse.class);
	}

	public BranchResponse createBranch(String franchiseId, String name) {
		return post("/api/franchises/" + franchiseId + "/branches", "{\"name\":\"" + name + "\"}", BranchResponse.class);
	}

	public BranchResponse createBranch() {
		return createBranch(createFranchise("Franquicia de prueba").id(), "Sucursal de prueba");
	}

	public ProductResponse addProduct(BranchResponse branch, String name, int stock) {
		return post("/api/franchises/" + branch.franchiseId() + "/branches/" + branch.id() + "/products",
				"{\"name\":\"" + name + "\",\"stock\":" + stock + "}", ProductResponse.class);
	}

	public WebTestClient.ResponseSpec patch(String uri, String json) {
		return client.patch().uri(uri)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange();
	}

	private <T> T post(String uri, String json, Class<T> type) {
		return client.post().uri(uri)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(json)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(type).returnResult().getResponseBody();
	}

}
