package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

/**
 * Publica en {@code /actuator/info} la versión de datos que esta instancia verificó al arrancar:
 * <pre>{"migrations": {"version": "V001", "appliedOnStartup": []}}</pre>
 * El CD la compara con la última migración del repositorio para confirmar el despliegue. Se
 * guarda en memoria para no consultar DynamoDB (ni bloquear) en cada llamada al endpoint.
 */
public class MigrationStatus implements InfoContributor {

	private volatile MigrationResult result;

	void update(MigrationResult result) {
		this.result = result;
	}

	@Override
	public void contribute(Info.Builder builder) {
		MigrationResult current = result;
		if (current == null || current.version() == null) {
			return;
		}
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("version", current.version());
		details.put("appliedOnStartup", current.applied());
		builder.withDetail("migrations", details);
	}

}
