package com.brayangutierrez.franchisesapi.modules.franchises.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
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

/**
 * Consistencia de los datos bajo concurrencia: nunca quedan productos fantasma, reservas
 * huérfanas ni nombres duplicados, y lo escrito se lee de inmediato.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(RawTable.class)
class DataConsistencyIT {

	private static final Set<Integer> ALLOWED = Set.of(200, 201, 204, 404, 409);

	@LocalServerPort
	private int port;

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private RawTable table;

	private ApiFixtures api;
	private ConcurrentClient concurrent;
	private IntegrityChecker integrity;

	@BeforeEach
	void setUp() {
		api = new ApiFixtures(webTestClient);
		concurrent = new ConcurrentClient(port);
		integrity = new IntegrityChecker(table);
	}

	@Test
	void deletingWhileStockAndNameChangeNeverLeavesAGhost() {
		BranchResponse branch = api.createBranch();
		List<ProductResponse> products = IntStream.range(0, 10)
				.mapToObj(index -> api.addProduct(branch, "Producto " + index, index))
				.toList();

		List<HttpStatusCode> statuses = concurrent.runTogether(
				Flux.fromIterable(products).flatMap(product -> concurrent.delete(uri(product))),
				Flux.range(0, 100).flatMap(n -> concurrent.patch(uri(products.get(n % 10)) + "/stock", "{\"stock\":" + n + "}")),
				Flux.range(0, 30).flatMap(n -> concurrent.patch(uri(products.get(n % 10)), "{\"name\":\"Otro " + n + "\"}")));

		assertThat(statuses).extracting(HttpStatusCode::value).allMatch(ALLOWED::contains);
		assertThat(table.items(branch.franchiseId(), "B#" + branch.id() + "#"))
				.as("ni productos fantasma ni reservas huérfanas")
				.isEmpty();
	}

	@Test
	void mixedConcurrentWorkloadKeepsProductsAndReservationsConsistent() {
		BranchResponse branch = api.createBranch();
		List<ProductResponse> products = IntStream.range(0, 15)
				.mapToObj(index -> api.addProduct(branch, "Item " + index, index))
				.toList();
		Random random = new Random(42);
		List<Integer> targets = random.ints(200, 0, products.size()).boxed().toList();
		List<Integer> names = random.ints(200, 0, 20).boxed().toList();

		List<HttpStatusCode> statuses = concurrent.runTogether(
				Flux.range(0, 40).flatMap(n -> concurrent.post(productsUri(branch),
						"{\"name\":\"Item " + names.get(n) + "\",\"stock\":1}")),
				Flux.range(40, 60).flatMap(n -> concurrent.patch(uri(products.get(targets.get(n))),
						"{\"name\":\"ITEM " + names.get(n) + "\"}")),
				Flux.range(100, 60).flatMap(n -> concurrent.patch(uri(products.get(targets.get(n))) + "/stock",
						"{\"stock\":" + n + "}")),
				Flux.range(160, 20).flatMap(n -> concurrent.delete(uri(products.get(targets.get(n))))));

		assertThat(statuses).extracting(HttpStatusCode::value).allMatch(ALLOWED::contains);
		integrity.assertConsistent(branch.franchiseId(), branch.id());
	}

	@Test
	void concurrentWritesToAMissingBranchLeaveNothingBehind() {
		BranchResponse branch = api.createBranch();
		String missingBranchId = UUID.randomUUID().toString();
		String uri = "/api/franchises/" + branch.franchiseId() + "/branches/" + missingBranchId + "/products";

		List<HttpStatusCode> statuses = concurrent.run(30, n -> concurrent.post(uri, "{\"name\":\"P " + n + "\",\"stock\":1}"));

		assertThat(statuses).extracting(HttpStatusCode::value).containsOnly(404);
		assertThat(table.items(branch.franchiseId(), "B#" + missingBranchId)).isEmpty();
	}

	@Test
	void everyWriteIsVisibleInTheNextRead() {
		BranchResponse branch = api.createBranch();
		ProductResponse product = api.addProduct(branch, "Café", 0);

		for (int stock = 1; stock <= 20; stock++) {
			api.patch(uri(product) + "/stock", "{\"stock\":" + stock + "}").expectStatus().isOk();
			webTestClient.get().uri("/api/franchises/{f}/top-stock-products", branch.franchiseId()).exchange()
					.expectStatus().isOk()
					.expectBody().jsonPath("$[0].stock").isEqualTo(stock);
		}
	}

	@Test
	void renamedAndDeletedNamesAreFreedForNewProducts() {
		BranchResponse branch = api.createBranch();
		ProductResponse product = api.addProduct(branch, "Café", 1);
		api.patch(uri(product), "{\"name\":\"Espresso\"}").expectStatus().isOk();
		webTestClient.delete().uri(uri(product)).exchange().expectStatus().isNoContent();

		api.addProduct(branch, "Café", 1);
		api.addProduct(branch, "Espresso", 1);
		integrity.assertConsistent(branch.franchiseId(), branch.id());
	}

	private static String productsUri(BranchResponse branch) {
		return "/api/franchises/" + branch.franchiseId() + "/branches/" + branch.id() + "/products";
	}

	private static String uri(ProductResponse product) {
		return "/api/franchises/" + product.franchiseId() + "/branches/" + product.branchId() + "/products/" + product.id();
	}

}
