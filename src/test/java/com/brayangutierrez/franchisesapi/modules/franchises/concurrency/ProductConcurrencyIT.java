package com.brayangutierrez.franchisesapi.modules.franchises.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Condiciones de carrera sobre productos, con peticiones HTTP simultáneas reales. Las aserciones
 * verifican invariantes (un solo ganador, valores válidos, sin datos huérfanos), nunca un orden
 * concreto, para que las pruebas sean estables.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(RawTable.class)
class ProductConcurrencyIT {

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
	void hundredSimultaneousStockUpdatesNeverFailAndLeaveOneOfTheSentValues() {
		BranchResponse branch = api.createBranch();
		ProductResponse product = api.addProduct(branch, "Café", 0);

		List<HttpStatusCode> statuses = concurrent.run(100, stock -> concurrent.patch(stockUri(product), stockBody(stock)));

		assertThat(statuses).hasSize(100).allMatch(HttpStatusCode::is2xxSuccessful);
		Map<String, AttributeValue> stored = table.product(product.franchiseId(), product.branchId(), product.id());
		assertThat(Integer.parseInt(stored.get("stock").n())).isBetween(1, 100);
		assertThat(stored.get("name").s()).isEqualTo("Café");
		assertThat(table.itemsOfType(branch.franchiseId(), branch.id(), "PRODUCT_NAME")).hasSize(1);
	}

	@Test
	void simultaneousStockUpdatesOnDifferentProductsDoNotInterfere() {
		BranchResponse branch = api.createBranch();
		List<ProductResponse> products = IntStream.range(0, 10)
				.mapToObj(index -> api.addProduct(branch, "Producto " + index, 0))
				.toList();

		List<HttpStatusCode> statuses = concurrent.run(200, request -> {
			int index = request % 10;
			return concurrent.patch(stockUri(products.get(index)), stockBody(index * 1000 + request));
		});

		assertThat(statuses).hasSize(200).allMatch(HttpStatusCode::is2xxSuccessful);
		for (int index = 0; index < products.size(); index++) {
			ProductResponse product = products.get(index);
			int stock = Integer.parseInt(table.product(branch.franchiseId(), branch.id(), product.id()).get("stock").n());
			assertThat(stock).as("stock de %s", product.name()).isBetween(index * 1000, index * 1000 + 200);
		}
	}

	@Test
	void renameAndStockChangesAtTheSameTimeDoNotOverwriteEachOther() {
		BranchResponse branch = api.createBranch();
		ProductResponse product = api.addProduct(branch, "Café", 0);

		List<HttpStatusCode> statuses = concurrent.runTogether(
				Flux.range(1, 50).flatMap(stock -> concurrent.patch(stockUri(product), stockBody(stock))),
				Flux.from(concurrent.patch(productUri(product), "{\"name\":\"Espresso\"}")));

		assertThat(statuses).hasSize(51).allMatch(HttpStatusCode::is2xxSuccessful);
		Map<String, AttributeValue> stored = table.product(product.franchiseId(), product.branchId(), product.id());
		assertThat(stored.get("name").s()).isEqualTo("Espresso");
		assertThat(Integer.parseInt(stored.get("stock").n())).isBetween(1, 50);
		assertThat(table.itemsOfType(branch.franchiseId(), branch.id(), "PRODUCT_NAME"))
				.singleElement()
				.satisfies(reservation -> assertThat(reservation.get("SK").s()).endsWith("#NAME#espresso"));
	}

	@Test
	void twentyIdenticalCreationsHaveExactlyOneWinner() {
		BranchResponse branch = api.createBranch();

		List<HttpStatusCode> statuses = concurrent.run(20, attempt -> concurrent.post(productsUri(branch),
				"{\"name\":\"Latte\",\"stock\":" + attempt + "}"));

		assertThat(count(statuses, 201)).isEqualTo(1);
		assertThat(count(statuses, 409)).isEqualTo(19);
		assertThat(table.itemsOfType(branch.franchiseId(), branch.id(), "PRODUCT")).hasSize(1);
		assertThat(table.itemsOfType(branch.franchiseId(), branch.id(), "PRODUCT_NAME")).hasSize(1);
	}

	@Test
	void twoProductsRenamedToTheSameNameAtOnceHaveExactlyOneWinner() {
		BranchResponse branch = api.createBranch();
		ProductResponse cafe = api.addProduct(branch, "Café", 1);
		ProductResponse te = api.addProduct(branch, "Té", 1);

		List<HttpStatusCode> statuses = concurrent.runTogether(
				Flux.from(concurrent.patch(productUri(cafe), "{\"name\":\"Mocha\"}")),
				Flux.from(concurrent.patch(productUri(te), "{\"name\":\"MOCHA\"}")));

		assertThat(statuses).extracting(HttpStatusCode::value).containsExactlyInAnyOrder(200, 409);
		List<String> names = table.itemsOfType(branch.franchiseId(), branch.id(), "PRODUCT").stream()
				.map(item -> item.get("name").s()).toList();
		List<String> reservations = table.itemsOfType(branch.franchiseId(), branch.id(), "PRODUCT_NAME").stream()
				.map(item -> item.get("SK").s().replaceAll(".*#NAME#", "")).toList();
		// Gana uno solo: o "Café" pasó a "Mocha" y "Té" quedó igual, o al revés.
		assertThat(Set.copyOf(names)).isIn(Set.of("Mocha", "Té"), Set.of("Café", "MOCHA"));
		assertThat(reservations).hasSize(2).contains("mocha").containsAnyOf("cafe", "te");
	}

	@Test
	void tenSimultaneousDeletesHaveExactlyOneWinnerAndLeaveNothing() {
		BranchResponse branch = api.createBranch();
		ProductResponse product = api.addProduct(branch, "Café", 5);

		List<HttpStatusCode> statuses = concurrent.run(10, attempt -> concurrent.delete(productUri(product)));

		assertThat(count(statuses, 204)).isEqualTo(1);
		assertThat(count(statuses, 404)).isEqualTo(9);
		assertThat(table.items(branch.franchiseId(), "B#" + branch.id() + "#")).isEmpty();
	}

	private static long count(List<HttpStatusCode> statuses, int code) {
		return statuses.stream().filter(status -> status.value() == code).count();
	}

	private static String productsUri(BranchResponse branch) {
		return "/api/franchises/" + branch.franchiseId() + "/branches/" + branch.id() + "/products";
	}

	private static String productUri(ProductResponse product) {
		return "/api/franchises/" + product.franchiseId() + "/branches/" + product.branchId() + "/products/" + product.id();
	}

	private static String stockUri(ProductResponse product) {
		return productUri(product) + "/stock";
	}

	private static String stockBody(int stock) {
		return "{\"stock\":" + stock + "}";
	}

}
