package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.franchise;

import org.springframework.stereotype.Repository;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Implementación de {@link FranchiseRepository} sobre la tabla única de DynamoDB.
 * Traduce los errores de AWS a errores de negocio: ninguna excepción del SDK sale de aquí
 * cuando tiene un significado para el negocio.
 */
@Repository
public class FranchiseDynamoRepository implements FranchiseRepository {

	/** Evita sobrescribir: el Put solo se aplica si no existe un ítem con esa clave. */
	private static final Expression IF_NOT_EXISTS = Expression.builder()
			.expression("attribute_not_exists(PK)")
			.build();

	private final DynamoDbAsyncTable<FranchiseItem> table;

	/** Crea una vista de la tabla única tipada con {@link FranchiseItem}. */
	public FranchiseDynamoRepository(DynamoDbEnhancedAsyncClient enhancedClient, DynamoDbProperties properties) {
		this.table = enhancedClient.table(properties.tableName(), TableSchema.fromBean(FranchiseItem.class));
	}

	@Override
	public Mono<Franchise> save(Franchise franchise) {
		PutItemEnhancedRequest<FranchiseItem> request = PutItemEnhancedRequest.builder(FranchiseItem.class)
				.item(FranchiseItemMapper.toItem(franchise))
				.conditionExpression(IF_NOT_EXISTS)
				.build();
		return Mono.fromFuture(() -> table.putItem(request))
				.thenReturn(franchise)
				.onErrorMap(ConditionalCheckFailedException.class,
						e -> new BusinessException(FranchiseError.FRANCHISE_ALREADY_EXISTS, franchise.id()));
	}

	@Override
	public Mono<Franchise> findById(String id) {
		GetItemEnhancedRequest request = GetItemEnhancedRequest.builder()
				.key(DynamoKeys.franchiseKey(id))
				.consistentRead(true)
				.build();
		return Mono.fromFuture(() -> table.getItem(request))
				.map(FranchiseItemMapper::toDomain);
	}

}
