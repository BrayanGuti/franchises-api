package com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class OpenApiDocumentationIT {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void apiDocsArePublished() {
		webTestClient.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.openapi").exists()
				.jsonPath("$.info.title").isEqualTo("Franchises API");
	}

	@Test
	void swaggerUiRedirectsToItsIndex() {
		webTestClient.get().uri("/swagger-ui.html")
				.exchange()
				.expectStatus().is3xxRedirection()
				.expectHeader().value("Location", location -> assertThat(location).contains("swagger-ui/index.html"));
	}

	@Test
	void swaggerUiIndexIsServed() {
		webTestClient.get().uri("/webjars/swagger-ui/index.html")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
	}

}
