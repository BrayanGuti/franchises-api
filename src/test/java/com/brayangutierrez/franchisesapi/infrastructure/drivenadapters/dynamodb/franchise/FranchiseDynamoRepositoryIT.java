package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.franchise;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.brayangutierrez.franchisesapi.domain.model.exception.BusinessError;
import com.brayangutierrez.franchisesapi.domain.model.exception.BusinessException;
import com.brayangutierrez.franchisesapi.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.config.DynamoDbProperties;

import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Prueba el adaptador contra dynamodb-test. Cada test usa IDs únicos, así no depende del
 * estado que dejen los demás y pueden correr en paralelo sin limpiar la tabla.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class FranchiseDynamoRepositoryIT {

	@Autowired
	private FranchiseRepository repository;

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void savedFranchiseCanBeFoundById() {
		Franchise franchise = new Franchise(uniqueId(), "Cafeterías Norte");

		StepVerifier.create(repository.save(franchise).then(repository.findById(franchise.id())))
				.expectNext(franchise)
				.verifyComplete();
	}

	@Test
	void findByIdIsEmptyWhenFranchiseDoesNotExist() {
		StepVerifier.create(repository.findById(uniqueId()))
				.verifyComplete();
	}

	@Test
	void savingAnExistingIdFailsAndDoesNotOverwrite() {
		String id = uniqueId();
		Franchise original = new Franchise(id, "Original");

		StepVerifier.create(repository.save(original).then(repository.save(new Franchise(id, "Intruso"))))
				.expectErrorSatisfies(error -> assertThat(error)
						.isInstanceOfSatisfying(BusinessException.class,
								e -> assertThat(e.error()).isEqualTo(BusinessError.FRANCHISE_ALREADY_EXISTS)))
				.verify();

		StepVerifier.create(repository.findById(id))
				.expectNext(original)
				.verifyComplete();
	}

	@Test
	void franchiseIsStoredWithSingleTableKeys() {
		String id = uniqueId();
		repository.save(new Franchise(id, "Cafeterías Norte")).block();

		Map<String, AttributeValue> raw = client.getItem(request -> request
				.tableName(properties.tableName())
				.key(Map.of(
						"PK", AttributeValue.fromS("F#" + id),
						"SK", AttributeValue.fromS("META"))))
				.join()
				.item();

		assertThat(raw.get("entityType").s()).isEqualTo("FRANCHISE");
		assertThat(raw.get("name").s()).isEqualTo("Cafeterías Norte");
		assertThat(raw).doesNotContainKey("stock");
	}

	private static String uniqueId() {
		return UUID.randomUUID().toString();
	}

}
