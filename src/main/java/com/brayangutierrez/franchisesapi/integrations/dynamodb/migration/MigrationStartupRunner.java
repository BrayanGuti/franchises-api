package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.DynamoTableInitializer;

/**
 * Aplica las migraciones pendientes al arrancar, justo después de crear la tabla en local.
 * <p>
 * Spring Boot marca la aplicación lista (readiness UP) solo cuando terminan todos los
 * {@link ApplicationRunner}, así que el balanceador no le envía tráfico a una instancia cuyos datos
 * aún no están en su versión. Si una migración falla, la app no arranca y ECS hace rollback.
 * Bloquear es aceptable: ocurre una vez, antes de recibir tráfico.
 */
public class MigrationStartupRunner implements ApplicationRunner, Ordered {

	private static final Logger log = LoggerFactory.getLogger(MigrationStartupRunner.class);

	private final MigrationRunner runner;
	private final MigrationStatus status;

	public MigrationStartupRunner(MigrationRunner runner, MigrationStatus status) {
		this.runner = runner;
		this.status = status;
	}

	@Override
	public void run(ApplicationArguments args) {
		MigrationResult result = runner.run().block();
		status.update(result);
		log.info("Datos en la versión {} ({} migraciones aplicadas ahora)", result.version(), result.applied().size());
	}

	@Override
	public int getOrder() {
		return DynamoTableInitializer.ORDER + 1;
	}

}
