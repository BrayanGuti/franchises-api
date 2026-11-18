package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.renamebranch;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.ApiFixtures;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RenameBranchIT {

	@Autowired
	private WebTestClient webTestClient;

	private ApiFixtures api;

	@BeforeEach
	void setUp() {
		api = new ApiFixtures(webTestClient);
	}

	@Test
	void renamesAnExistingBranch() {
		BranchResponse branch = api.createBranch();

		api.patch(uri(branch.franchiseId(), branch.id()), "{\"name\":\"Norte\"}")
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isEqualTo(branch.id())
				.jsonPath("$.franchiseId").isEqualTo(branch.franchiseId())
				.jsonPath("$.name").isEqualTo("Norte");
	}

	@Test
	void unknownBranchReturns404() {
		BranchResponse branch = api.createBranch();

		api.patch(uri(branch.franchiseId(), UUID.randomUUID().toString()), "{\"name\":\"Norte\"}")
				.expectStatus().isNotFound();
	}

	@Test
	void endpointIsDocumentedInOpenApi() {
		webTestClient.get().uri("/v3/api-docs").exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.paths['/api/franchises/{franchiseId}/branches/{branchId}'].patch.summary")
				.isEqualTo("Actualizar el nombre de una sucursal");
	}

	private static String uri(String franchiseId, String branchId) {
		return "/api/franchises/" + franchiseId + "/branches/" + branchId;
	}

}
