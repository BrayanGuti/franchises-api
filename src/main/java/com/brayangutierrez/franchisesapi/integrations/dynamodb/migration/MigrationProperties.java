package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de las migraciones ({@code app.migrations}).
 *
 * @param enabled aplicar las migraciones pendientes al arrancar (activo por defecto en todos los
 *                entornos: la app no se marca lista hasta tener los datos en su versión).
 * @param lockTtl vencimiento del lock; debe superar la duración de la migración más larga.
 */
@ConfigurationProperties(prefix = "app.migrations")
public record MigrationProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("5m") Duration lockTtl) {
}
