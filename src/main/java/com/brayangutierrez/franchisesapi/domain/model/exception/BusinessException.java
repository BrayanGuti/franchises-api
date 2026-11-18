package com.brayangutierrez.franchisesapi.domain.model.exception;

/**
 * Única excepción de negocio de la aplicación. La lanzan los casos de uso y los
 * adaptadores (traduciendo los errores técnicos, por ejemplo de DynamoDB).
 */
public class BusinessException extends RuntimeException {

	private final BusinessError error;

	public BusinessException(BusinessError error, Object... args) {
		super(error.message(args));
		this.error = error;
	}

	public BusinessError error() {
		return error;
	}

}
