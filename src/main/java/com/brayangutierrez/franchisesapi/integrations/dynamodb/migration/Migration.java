package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import reactor.core.publisher.Mono;

/**
 * Migración de datos versionada, al estilo de Flyway: cada implementación es un bean de Spring cuyo
 * nombre de clase define la versión y la descripción ({@code V002__BackfillProductCategory}).
 * <p>
 * Reglas para escribir una migración:
 * <ul>
 * <li><b>Idempotente y reanudable</b>: si la instancia muere a mitad de camino, la migración se
 * vuelve a ejecutar completa. Cada escritura debe poder repetirse sin efecto (por ejemplo, con la
 * condición {@code attribute_not_exists(nuevoAtributo)}) y procesar los datos por páginas.</li>
 * <li><b>Expand/contract</b>: durante un despliegue conviven la versión vieja y la nueva de la app.
 * Primero se agrega lo nuevo sin quitar lo viejo (expand); solo cuando ninguna instancia vieja lo
 * usa, otra migración posterior lo elimina (contract).</li>
 * <li><b>Nunca se edita una migración ya desplegada</b>: los cambios van en una versión nueva.</li>
 * </ul>
 * La estructura de la tabla (claves, índices, TTL) no se migra aquí: en AWS la define Terraform.
 */
public interface Migration {

	/** Aplica la migración. Debe completar sin valor o terminar con error. */
	Mono<Void> migrate();

	/** Nombre con formato {@code V<3 dígitos>__<Descripcion>}; por defecto, el nombre de la clase. */
	default String name() {
		return getClass().getSimpleName();
	}

}
