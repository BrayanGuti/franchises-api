package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch;

import org.springframework.stereotype.Repository;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.DynamoConditions;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Implementación de {@link BranchRepository} sobre la tabla única de DynamoDB.
 */
@Repository
public class BranchDynamoRepository implements BranchRepository {

	/** Posición de cada operación dentro de la transacción de alta. */
	private static final int FRANCHISE_CHECK = 0;
	private static final int BRANCH_PUT = 1;

	private final DynamoDbEnhancedAsyncClient enhancedClient;
	private final DynamoDbAsyncTable<BranchItem> table;

	public BranchDynamoRepository(DynamoDbEnhancedAsyncClient enhancedClient, DynamoDbProperties properties) {
		this.enhancedClient = enhancedClient;
		this.table = enhancedClient.table(properties.tableName(), TableSchema.fromBean(BranchItem.class));
	}

	/**
	 * Una sola transacción: comprueba que la franquicia exista y crea la sucursal. Si una de las
	 * dos condiciones falla, DynamoDB no aplica ninguna escritura.
	 */
	@Override
	public Mono<Branch> save(Branch branch) {
		TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
				.addConditionCheck(table, ConditionCheck.builder()
						.key(DynamoKeys.franchiseKey(branch.franchiseId()))
						.conditionExpression(DynamoConditions.ITEM_EXISTS)
						.build())
				.addPutItem(table, TransactPutItemEnhancedRequest.builder(BranchItem.class)
						.item(BranchItemMapper.toItem(branch))
						.conditionExpression(DynamoConditions.ITEM_NOT_EXISTS)
						.build())
				.build();
		return Mono.fromFuture(() -> enhancedClient.transactWriteItems(request))
				.thenReturn(branch)
				.onErrorMap(TransactionCanceledException.class, e -> translate(e, branch));
	}

	private static Throwable translate(TransactionCanceledException exception, Branch branch) {
		if (DynamoConditions.failedAt(exception, FRANCHISE_CHECK)) {
			return new BusinessException(FranchiseError.FRANCHISE_NOT_FOUND, branch.franchiseId());
		}
		if (DynamoConditions.failedAt(exception, BRANCH_PUT)) {
			return new BusinessException(FranchiseError.BRANCH_ALREADY_EXISTS, branch.id());
		}
		return exception;
	}

}
