package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Condiciones sobre el estado de una sucursal.
 * <p>
 * Una sucursal sin atributo {@code status} se considera activa. Cuando se elimina, primero se marca
 * como {@code DELETING}: desde ese momento las operaciones que crean o cambian ítems bajo la
 * sucursal deben fallar, para que el borrado no deje datos huérfanos.
 */
public final class BranchConditions {

	public static final String STATUS_ATTRIBUTE = "status";
	public static final String ACTIVE = "ACTIVE";
	public static final String DELETING = "DELETING";

	private BranchConditions() {
	}

	/** La sucursal existe y no se está eliminando. */
	public static Expression existsAndActive() {
		return Expression.builder()
				.expression("attribute_exists(PK) AND (attribute_not_exists(#status) OR #status = :active)")
				.putExpressionName("#status", STATUS_ATTRIBUTE)
				.putExpressionValue(":active", AttributeValue.fromS(ACTIVE))
				.build();
	}

}
