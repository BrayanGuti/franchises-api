package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import reactor.test.StepVerifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class BranchDynamoRepositoryIT {

	@Autowired
	private BranchRepository branchRepository;

	@Autowired
	private FranchiseRepository franchiseRepository;

	@Autowired
	private DynamoDbAsyncClient client;

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void savesBranchInsideItsFranchisePartition() {
		String franchiseId = existingFranchise();
		Branch branch = new Branch(uniqueId(), franchiseId, "Sucursal Centro");

		StepVerifier.create(branchRepository.save(branch))
				.expectNext(branch)
				.verifyComplete();

		Map<String, AttributeValue> raw = rawItem("F#" + franchiseId, "B#" + branch.id());
		assertThat(raw.get("entityType").s()).isEqualTo("BRANCH");
		assertThat(raw.get("name").s()).isEqualTo("Sucursal Centro");
	}

	@Test
	void failsWhenFranchiseDoesNotExistAndLeavesNoOrphan() {
		String missingFranchiseId = uniqueId();
		Branch branch = new Branch(uniqueId(), missingFranchiseId, "Huérfana");

		StepVerifier.create(branchRepository.save(branch))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.FRANCHISE_NOT_FOUND))
				.verify();

		assertThat(rawItem("F#" + missingFranchiseId, "B#" + branch.id())).isEmpty();
	}

	@Test
	void savingAnExistingBranchIdFailsAndDoesNotOverwrite() {
		String franchiseId = existingFranchise();
		String branchId = uniqueId();
		branchRepository.save(new Branch(branchId, franchiseId, "Original")).block();

		StepVerifier.create(branchRepository.save(new Branch(branchId, franchiseId, "Intruso")))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.BRANCH_ALREADY_EXISTS))
				.verify();

		assertThat(rawItem("F#" + franchiseId, "B#" + branchId).get("name").s()).isEqualTo("Original");
	}

	private String existingFranchise() {
		return franchiseRepository.save(new Franchise(uniqueId(), "Franquicia de prueba")).block().id();
	}

	private Map<String, AttributeValue> rawItem(String pk, String sk) {
		return client.getItem(request -> request
				.tableName(properties.tableName())
				.consistentRead(true)
				.key(Map.of("PK", AttributeValue.fromS(pk), "SK", AttributeValue.fromS(sk))))
				.join()
				.item();
	}

	private static String uniqueId() {
		return UUID.randomUUID().toString();
	}

}
