package com.brayangutierrez.franchisesapi.modules.franchises.concurrency;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.ApiFixtures;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Comprueba que {@link IntegrityChecker} detecta datos inconsistentes: un verificador que nunca
 * falla no probaría nada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import(RawTable.class)
class IntegrityCheckerIT {

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private RawTable table;

	private ApiFixtures api;
	private IntegrityChecker integrity;

	@BeforeEach
	void setUp() {
		api = new ApiFixtures(webTestClient);
		integrity = new IntegrityChecker(table);
	}

	@Test
	void acceptsConsistentData() {
		BranchResponse branch = api.createBranch();
		api.addProduct(branch, "Café", 1);

		assertThatCode(() -> integrity.assertConsistent(branch.franchiseId(), branch.id())).doesNotThrowAnyException();
	}

	@Test
	void detectsAProductWithoutItsReservation() {
		BranchResponse branch = api.createBranch();
		api.addProduct(branch, "Café", 1);
		table.delete("F#" + branch.franchiseId(), "B#" + branch.id() + "#NAME#cafe");

		assertThatThrownBy(() -> integrity.assertConsistent(branch.franchiseId(), branch.id()))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	void detectsAnOrphanReservation() {
		BranchResponse branch = api.createBranch();
		api.addProduct(branch, "Café", 1);
		table.put(Map.of(
				"PK", AttributeValue.fromS("F#" + branch.franchiseId()),
				"SK", AttributeValue.fromS("B#" + branch.id() + "#NAME#fantasma"),
				"entityType", AttributeValue.fromS("PRODUCT_NAME"),
				"productId", AttributeValue.fromS("no-existe")));

		assertThatThrownBy(() -> integrity.assertConsistent(branch.franchiseId(), branch.id()))
				.isInstanceOf(AssertionError.class);
	}

}
