package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

/** Error al validar o ejecutar las migraciones; detiene el arranque de la aplicación. */
public class MigrationException extends RuntimeException {

	public MigrationException(String message) {
		super(message);
	}

	public MigrationException(String message, Throwable cause) {
		super(message, cause);
	}

}
