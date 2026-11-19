package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Ejecuta las migraciones pendientes en orden de versión, al estilo de Flyway:
 * <ol>
 * <li>Valida los nombres y que no haya versiones repetidas (antes de tocar la base).</li>
 * <li>Toma el lock distribuido.</li>
 * <li>Lee el historial y aplica, una a una, las versiones que no están registradas; cada una se
 * registra apenas termina, así que un fallo no repite las anteriores.</li>
 * <li>Libera el lock siempre, también si una migración falla.</li>
 * </ol>
 * Las versiones registradas que esta instancia no conoce (una versión más nueva de la app ya
 * migró, por ejemplo durante un rollback) se ignoran: con expand/contract el código viejo sigue
 * funcionando sobre datos más nuevos.
 */
public class MigrationRunner {

	private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

	private final NavigableMap<MigrationVersion, Migration> migrations;
	private final MigrationLock lock;
	private final MigrationHistory history;
	private final Clock clock;

	public MigrationRunner(List<Migration> migrations, MigrationLock lock, MigrationHistory history, Clock clock) {
		this.migrations = byVersion(migrations);
		this.lock = lock;
		this.history = history;
		this.clock = clock;
	}

	public Mono<MigrationResult> run() {
		return Mono.usingWhen(lock.acquire().thenReturn(lock),
				held -> history.appliedVersions().flatMap(this::applyPending),
				MigrationLock::release);
	}

	/** Última versión que esta instancia conoce; es la que exige para funcionar. */
	public String latestVersion() {
		return migrations.isEmpty() ? null : migrations.lastKey().label();
	}

	private Mono<MigrationResult> applyPending(Set<String> applied) {
		return Flux.fromIterable(migrations.entrySet())
				.filter(entry -> !applied.contains(entry.getKey().label()))
				.concatMap(entry -> apply(entry.getKey(), entry.getValue()))
				.collectList()
				.map(newlyApplied -> new MigrationResult(newlyApplied, latestVersion()));
	}

	private Mono<String> apply(MigrationVersion version, Migration migration) {
		return Mono.defer(() -> {
			String name = migration.name();
			Instant start = clock.instant();
			log.info("Aplicando migración {}", name);
			return migration.migrate()
					.onErrorMap(error -> new MigrationException("Falló la migración " + name, error))
					.then(Mono.defer(() -> history.record(version, Duration.between(start, clock.instant()))))
					.doOnSuccess(done -> log.info("Migración {} aplicada", name))
					.thenReturn(version.label());
		});
	}

	private static NavigableMap<MigrationVersion, Migration> byVersion(List<Migration> migrations) {
		TreeMap<MigrationVersion, Migration> sorted = new TreeMap<>();
		for (Migration migration : migrations) {
			MigrationVersion version = MigrationVersion.of(migration);
			Migration previous = sorted.put(version, migration);
			if (previous != null) {
				throw new MigrationException("Versión de migración repetida " + version.label() + ": "
						+ previous.name() + " y " + migration.name());
			}
		}
		return sorted;
	}

}
