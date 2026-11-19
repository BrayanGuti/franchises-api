package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Versión y descripción de una migración, obtenidas de su nombre: {@code V001__Baseline} es la
 * versión 1 con descripción {@code Baseline}. Siempre tres dígitos, para que el orden alfabético
 * de los archivos coincida con el orden de ejecución.
 */
public record MigrationVersion(int number, String description) implements Comparable<MigrationVersion> {

	private static final Pattern NAME = Pattern.compile("V(\\d{3})__([A-Za-z0-9]+)");

	public static MigrationVersion of(Migration migration) {
		return parse(migration.name());
	}

	static MigrationVersion parse(String name) {
		Matcher matcher = NAME.matcher(name);
		if (!matcher.matches()) {
			throw new MigrationException("Nombre de migración inválido '" + name
					+ "': se espera V<3 dígitos>__<Descripcion>, por ejemplo V002__BackfillCategory");
		}
		int number = Integer.parseInt(matcher.group(1));
		if (number == 0) {
			throw new MigrationException("La versión de '" + name + "' debe empezar en V001");
		}
		return new MigrationVersion(number, matcher.group(2));
	}

	/** Etiqueta con la que se guarda en el historial y se publica en /actuator/info: {@code V001}. */
	public String label() {
		return "V%03d".formatted(number);
	}

	@Override
	public int compareTo(MigrationVersion other) {
		return Integer.compare(number, other.number);
	}

}
