package com.brayangutierrez.franchisesapi.modules.franchises.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.ApiFixtures;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto.ProductResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Eliminar una sucursal mientras llegan peticiones sobre sus productos: lo que se confirma antes de
 * la marca se elimina con ella, lo posterior responde 404 y nunca quedan ítems huérfanos. Cuántas
 * peticiones caen antes o después depende del momento; las aserciones no dependen de eso.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(RawTable.class)
class BranchDeletionConcurrencyIT {

	@LocalServerPort
	private int port;

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private RawTable table;

	private ApiFixtures api;
	private ConcurrentClient concurrent;

	@BeforeEach
	void setUp() {
		api = new ApiFixtures(webTestClient);
		concurrent = new ConcurrentClient(port);
	}

	@Test
	void deletingABranchWhileRequestsArriveLeavesNoOrphans() {
		String franchiseId = api.createFranchise("Cafeterías").id();
		BranchResponse centro = api.createBranch(franchiseId, "Centro");
		BranchResponse norte = api.createBranch(franchiseId, "Norte");
		api.addProduct(norte, "Pan", 30);
		List<ProductResponse> products = IntStream.range(0, 20)
				.mapToObj(index -> api.addProduct(centro, "Producto " + index, index))
				.toList();
		String productsUri = "/api/franchises/" + franchiseId + "/branches/" + centro.id() + "/products";

		List<HttpStatusCode> others = concurrent.runTogether(
				Flux.range(1, 60).flatMap(n -> concurrent.post(productsUri, "{\"name\":\"Nuevo " + n + "\",\"stock\":1}")),
				Flux.range(0, 60).flatMap(n -> concurrent.patch(
						productsUri + "/" + products.get(n % 20).id() + "/stock", "{\"stock\":" + n + "}")),
				Flux.range(0, 20).flatMap(n -> concurrent.patch(
						productsUri + "/" + products.get(n).id(), "{\"name\":\"Renombrado " + n + "\"}")),
				Flux.from(Mono.delay(Duration.ofMillis(150))
						.then(concurrent.delete("/api/franchises/" + franchiseId + "/branches/" + centro.id())))
						.map(status -> status.value() == 204 ? status : HttpStatusCode.valueOf(599)));

		assertThat(others).extracting(HttpStatusCode::value)
				.as("la eliminación responde 204 y cada petición se completa o recibe 404")
				.contains(204)
				.doesNotContain(599)
				.allMatch(code -> Set.of(200, 201, 204, 404).contains(code));
		assertThat(table.items(franchiseId, "B#" + centro.id())).as("ítems huérfanos de la sucursal").isEmpty();
		assertThat(table.items(franchiseId, "B#" + norte.id())).as("la otra sucursal queda intacta").hasSize(3);
	}

	@Test
	void simultaneousDeletesOfTheSameBranchHaveExactlyOneWinner() {
		BranchResponse branch = api.createBranch();
		IntStream.range(0, 5).forEach(index -> api.addProduct(branch, "Producto " + index, index));
		String branchUri = "/api/franchises/" + branch.franchiseId() + "/branches/" + branch.id();

		List<HttpStatusCode> statuses = concurrent.run(5, attempt -> concurrent.delete(branchUri));

		assertThat(statuses).extracting(HttpStatusCode::value).containsOnly(204, 404).containsOnlyOnce(204);
		assertThat(table.items(branch.franchiseId(), "B#" + branch.id())).isEmpty();
	}

}
