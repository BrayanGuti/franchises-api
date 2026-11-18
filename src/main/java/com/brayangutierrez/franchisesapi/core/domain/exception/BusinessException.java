package com.brayangutierrez.franchisesapi.core.domain.exception;

/**
 * Única excepción de negocio de la aplicación. La lanzan los casos de uso y los
 * adaptadores (traduciendo los errores técnicos, por ejemplo de DynamoDB) con un error
 * del catálogo de su módulo.
 */
public class BusinessException extends RuntimeException {

	private final BusinessErrorCode error;

	public BusinessException(BusinessErrorCode error, Object... args) {
		super(error.message(args));
		this.error = error;
	}

	public BusinessErrorCode error() {
		return error;
	}

}
