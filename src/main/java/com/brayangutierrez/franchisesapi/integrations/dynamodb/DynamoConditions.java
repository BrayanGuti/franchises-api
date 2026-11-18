package com.brayangutierrez.franchisesapi.integrations.dynamodb;

import java.util.List;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Condiciones reutilizables y lectura de transacciones canceladas.
 */
public final class DynamoConditions {

	/** El ítem debe existir: evita crear ítems fantasma al actualizar o validar referencias. */
	public static final Expression ITEM_EXISTS = Expression.builder()
			.expression("attribute_exists(PK)")
			.build();

	/** El ítem no debe existir: evita sobrescribir al crear. */
	public static final Expression ITEM_NOT_EXISTS = Expression.builder()
			.expression("attribute_not_exists(PK)")
			.build();

	private static final String CONDITIONAL_CHECK_FAILED = "ConditionalCheckFailed";

	private DynamoConditions() {
	}

	/**
	 * Indica si la operación en la posición {@code index} de una transacción fue la que falló
	 * por su condición. DynamoDB devuelve un motivo por cada operación, en el mismo orden.
	 */
	public static boolean failedAt(TransactionCanceledException exception, int index) {
		List<CancellationReason> reasons = exception.cancellationReasons();
		return index < reasons.size() && CONDITIONAL_CHECK_FAILED.equals(reasons.get(index).code());
	}

}
