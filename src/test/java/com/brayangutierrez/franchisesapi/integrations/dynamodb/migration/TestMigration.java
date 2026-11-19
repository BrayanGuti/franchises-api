package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.util.function.Supplier;

import reactor.core.publisher.Mono;

/** Migración de prueba con nombre explícito; la acción se evalúa en cada ejecución. */
record TestMigration(String name, Supplier<Mono<Void>> action) implements Migration {

	@Override
	public Mono<Void> migrate() {
		return Mono.defer(action);
	}

}
