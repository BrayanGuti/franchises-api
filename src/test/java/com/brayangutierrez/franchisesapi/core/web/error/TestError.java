package com.brayangutierrez.franchisesapi.core.web.error;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessErrorCode;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessErrorType;

/**
 * Catálogo de errores solo para probar el manejador: así {@code core} no depende de ningún
 * módulo de negocio, ni siquiera en sus pruebas.
 */
enum TestError implements BusinessErrorCode {

	RESOURCE_NOT_FOUND(BusinessErrorType.NOT_FOUND, "La franquicia '%s' no existe"),
	RESOURCE_ALREADY_EXISTS(BusinessErrorType.ALREADY_EXISTS, "El producto '%s' ya existe en la sucursal"),
	CONCURRENT_CHANGE(BusinessErrorType.CONFLICT, "El producto '%s' cambió mientras se procesaba la solicitud"),
	INVALID_VALUE(BusinessErrorType.INVALID, "El stock no puede ser negativo: %s");

	private final BusinessErrorType type;
	private final String messageTemplate;

	TestError(BusinessErrorType type, String messageTemplate) {
		this.type = type;
		this.messageTemplate = messageTemplate;
	}

	@Override
	public BusinessErrorType type() {
		return type;
	}

	@Override
	public String message(Object... args) {
		return messageTemplate.formatted(args);
	}

}
