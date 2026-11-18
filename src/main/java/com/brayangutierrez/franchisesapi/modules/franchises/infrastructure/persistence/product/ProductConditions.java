package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Condiciones que comparten las operaciones que leen un producto y luego escriben (borrar,
 * renombrar): la escritura solo se aplica si nada cambió desde la lectura.
 */
final class ProductConditions {

	private ProductConditions() {
	}

	/** El producto sigue existiendo y con el mismo nombre que se leyó. */
	static Expression existsWithName(String name) {
		return Expression.builder()
				.expression("attribute_exists(PK) AND #name = :name")
				.putExpressionName("#name", "name")
				.putExpressionValue(":name", AttributeValue.fromS(name))
				.build();
	}

	/** La reserva de nombre no existe o pertenece a este producto: nunca se toca la de otro. */
	static Expression reservationMissingOrOwnedBy(String productId) {
		return Expression.builder()
				.expression("attribute_not_exists(PK) OR productId = :productId")
				.putExpressionValue(":productId", AttributeValue.fromS(productId))
				.build();
	}

}
