package com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessErrorCode;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessErrorType;

/**
 * Catálogo de errores del módulo de franquicias. Agregar un error nuevo es agregar una constante:
 * no hace falta crear clases ni tocar el manejador de errores.
 * <p>
 * Los mensajes admiten parámetros con el formato de {@link String#formatted(Object...)}.
 */
public enum FranchiseError implements BusinessErrorCode {

	FRANCHISE_NOT_FOUND(BusinessErrorType.NOT_FOUND, "La franquicia '%s' no existe"),
	FRANCHISE_ALREADY_EXISTS(BusinessErrorType.ALREADY_EXISTS, "La franquicia '%s' ya existe"),
	BRANCH_NOT_FOUND(BusinessErrorType.NOT_FOUND, "La sucursal '%s' no existe"),
	BRANCH_ALREADY_EXISTS(BusinessErrorType.ALREADY_EXISTS, "La sucursal '%s' ya existe"),
	BRANCH_DELETION_INCOMPLETE(BusinessErrorType.CONFLICT,
			"La sucursal '%s' no terminó de eliminarse; vuelve a intentarlo"),
	PRODUCT_NOT_FOUND(BusinessErrorType.NOT_FOUND, "El producto '%s' no existe"),
	PRODUCT_ALREADY_EXISTS(BusinessErrorType.ALREADY_EXISTS, "El producto '%s' ya existe en la sucursal"),
	PRODUCT_MODIFIED_CONCURRENTLY(BusinessErrorType.CONFLICT,
			"El producto '%s' cambió mientras se procesaba la solicitud; inténtalo de nuevo"),
	INVALID_STOCK(BusinessErrorType.INVALID, "El stock no puede ser negativo: %s"),
	INVALID_NAME(BusinessErrorType.INVALID, "El nombre es obligatorio");

	private final BusinessErrorType type;
	private final String messageTemplate;

	FranchiseError(BusinessErrorType type, String messageTemplate) {
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
