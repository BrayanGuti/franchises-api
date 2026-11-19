package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.util.List;

/**
 * Resultado de una ejecución: las versiones aplicadas ahora (vacío si no había pendientes) y la
 * versión de datos que la instancia verificó, es decir, la última que conoce su código.
 */
public record MigrationResult(List<String> applied, String version) {

	public MigrationResult {
		applied = List.copyOf(applied);
	}

}
