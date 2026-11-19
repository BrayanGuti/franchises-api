package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.migration;

import org.springframework.stereotype.Component;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.migration.Migration;

import reactor.core.publisher.Mono;

/**
 * Línea base, como el {@code baseline} de Flyway: representa el modelo de datos con el que nace la
 * aplicación, así que no transforma nada. Desde aquí, cada cambio en el formato de los ítems es una
 * migración nueva y pequeña ({@code V002__...}) en este mismo paquete.
 * <p>
 * Modelo de datos en esta versión (tabla única, claves en {@code DynamoKeys}):
 * <ul>
 * <li>Franquicia: {@code PK=F#<id>, SK=META}.</li>
 * <li>Sucursal: {@code PK=F#<id>, SK=B#<id>}, con {@code status=DELETING} mientras se elimina.</li>
 * <li>Producto: {@code PK=F#<id>, SK=B#<id>#P#<id>}.</li>
 * <li>Reserva de nombre de producto: {@code PK=F#<id>, SK=B#<id>#NAME#<nombre normalizado>}.</li>
 * </ul>
 * La tabla y sus claves no se crean aquí: en AWS las define Terraform y en local
 * {@code DynamoTableInitializer}, igual que Terraform. El historial vive en la propia tabla, así
 * que la tabla debe existir antes de migrar.
 */
@Component
class V001__Baseline implements Migration {

	@Override
	public Mono<Void> migrate() {
		return Mono.empty();
	}

}
