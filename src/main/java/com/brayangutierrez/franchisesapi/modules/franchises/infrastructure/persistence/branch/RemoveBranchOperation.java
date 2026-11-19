package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Elimina una sucursal y todo lo que cuelga de ella (productos y reservas de nombre) sin dejar
 * huérfanos, aunque haya peticiones simultáneas:
 * <ol>
 * <li><b>Lápida:</b> marca la sucursal {@code status = DELETING}. Desde ese instante, agregar o
 * renombrar productos falla (exigen sucursal activa en su transacción), así que nada nuevo puede
 * crearse bajo ella. Lo confirmado antes de la marca "completa" y se elimina en el barrido.</li>
 * <li><b>Barrido:</b> consulta los ítems {@code B#{branchId}#...} y los borra; repite hasta que no
 * quede ninguno.</li>
 * <li><b>Borrado final:</b> elimina la sucursal, solo si sigue marcada.</li>
 * </ol>
 * Es idempotente: si se interrumpe, la sucursal queda marcada y repetir la operación la completa.
 * No se espera en memoria a que terminen las peticiones: con varias instancias de la aplicación
 * eso no sería confiable; la marca vive en DynamoDB y la respetan todas.
 */
@Component
class RemoveBranchOperation {

	static final int MAX_SWEEPS = 5;
	private static final int DELETE_CONCURRENCY = 16;

	private final DynamoDbAsyncClient client;
	private final String tableName;

	RemoveBranchOperation(DynamoDbAsyncClient client, DynamoDbProperties properties) {
		this.client = client;
		this.tableName = properties.tableName();
	}

	Mono<Void> execute(String franchiseId, String branchId) {
		return markDeleting(franchiseId, branchId)
				.then(sweep(franchiseId, branchId, 1))
				.then(deleteBranch(franchiseId, branchId));
	}

	/** Condición: solo que exista, así un borrado interrumpido (ya marcado) se puede retomar. */
	private Mono<Void> markDeleting(String franchiseId, String branchId) {
		return Mono.fromFuture(() -> client.updateItem(request -> request
				.tableName(tableName)
				.key(branchKey(franchiseId, branchId))
				.updateExpression("SET #status = :deleting")
				.conditionExpression("attribute_exists(PK)")
				.expressionAttributeNames(Map.of("#status", BranchConditions.STATUS_ATTRIBUTE))
				.expressionAttributeValues(Map.of(":deleting", AttributeValue.fromS(BranchConditions.DELETING)))))
				.then()
				.onErrorMap(ConditionalCheckFailedException.class,
						e -> new BusinessException(FranchiseError.BRANCH_NOT_FOUND, branchId));
	}

	private Mono<Void> sweep(String franchiseId, String branchId, int pass) {
		return children(franchiseId, branchId)
				.collectList()
				.flatMap(items -> {
					if (items.isEmpty()) {
						return Mono.<Void>empty();
					}
					if (pass > MAX_SWEEPS) {
						return Mono.error(new BusinessException(FranchiseError.BRANCH_DELETION_INCOMPLETE, branchId));
					}
					return Flux.fromIterable(items)
							.flatMap(this::deleteItem, DELETE_CONCURRENCY)
							.then(Mono.defer(() -> sweep(franchiseId, branchId, pass + 1)));
				});
	}

	/** Productos y reservas de la sucursal: todo lo que tiene la sort key {@code B#{branchId}#...}. */
	private Flux<Map<String, AttributeValue>> children(String franchiseId, String branchId) {
		return Flux.from(client.queryPaginator(request -> request
				.tableName(tableName)
				.consistentRead(true)
				.keyConditionExpression("PK = :pk AND begins_with(SK, :prefix)")
				.projectionExpression("PK, SK")
				.expressionAttributeValues(Map.of(
						":pk", AttributeValue.fromS(DynamoKeys.franchisePk(franchiseId)),
						":prefix", AttributeValue.fromS(DynamoKeys.branchSk(branchId) + "#")))).items());
	}

	private Mono<Void> deleteItem(Map<String, AttributeValue> item) {
		return Mono.fromFuture(() -> client.deleteItem(request -> request
				.tableName(tableName)
				.key(Map.of("PK", item.get("PK"), "SK", item.get("SK")))))
				.then();
	}

	/** Si la condición falla, otra petición ya terminó de eliminar esta sucursal. */
	private Mono<Void> deleteBranch(String franchiseId, String branchId) {
		return Mono.fromFuture(() -> client.deleteItem(request -> request
				.tableName(tableName)
				.key(branchKey(franchiseId, branchId))
				.conditionExpression("#status = :deleting")
				.expressionAttributeNames(Map.of("#status", BranchConditions.STATUS_ATTRIBUTE))
				.expressionAttributeValues(Map.of(":deleting", AttributeValue.fromS(BranchConditions.DELETING)))))
				.then()
				.onErrorMap(ConditionalCheckFailedException.class,
						e -> new BusinessException(FranchiseError.BRANCH_NOT_FOUND, branchId));
	}

	private static Map<String, AttributeValue> branchKey(String franchiseId, String branchId) {
		return Map.of(
				"PK", AttributeValue.fromS(DynamoKeys.franchisePk(franchiseId)),
				"SK", AttributeValue.fromS(DynamoKeys.branchSk(branchId)));
	}

}
