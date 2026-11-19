package com.brayangutierrez.franchisesapi.modules.franchises.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Verifica la integridad referencial entre productos y reservas de nombre de una sucursal:
 * <ul>
 * <li>cada producto tiene exactamente su reserva (la de su nombre normalizado, apuntando a él);</li>
 * <li>cada reserva apunta a un producto que existe y que tiene ese nombre;</li>
 * <li>no hay dos productos con el mismo nombre normalizado.</li>
 * </ul>
 * Usa la misma normalización que la aplicación ({@link DynamoKeys#productNameSk}).
 */
final class IntegrityChecker {

	private final RawTable table;

	IntegrityChecker(RawTable table) {
		this.table = table;
	}

	void assertConsistent(String franchiseId, String branchId) {
		List<Map<String, AttributeValue>> products = table.itemsOfType(franchiseId, branchId, "PRODUCT");
		Map<String, String> reservationOwners = table.itemsOfType(franchiseId, branchId, "PRODUCT_NAME").stream()
				.collect(Collectors.toMap(item -> item.get("SK").s(), item -> item.get("productId").s()));

		assertThat(reservationOwners).as("una reserva por producto").hasSameSizeAs(products);
		for (Map<String, AttributeValue> product : products) {
			String productId = DynamoKeys.productIdFrom(product.get("SK").s());
			String expectedReservation = DynamoKeys.productNameSk(branchId, product.get("name").s());
			assertThat(reservationOwners)
					.as("el producto '%s' tiene su reserva y le pertenece", product.get("name").s())
					.containsEntry(expectedReservation, productId);
		}
		assertThat(products.stream().map(product -> DynamoKeys.productNameSk(branchId, product.get("name").s())))
				.as("no hay dos productos con el mismo nombre")
				.doesNotHaveDuplicates();
	}

}
