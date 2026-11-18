package com.brayangutierrez.franchisesapi.domain.model.exception;

/**
 * Catálogo de errores de negocio. Agregar un error nuevo es agregar una constante:
 * no hace falta crear clases ni tocar el manejador de errores.
 * <p>
 * Los mensajes admiten parámetros con el formato de {@link String#formatted(Object...)}.
 */
public enum BusinessError {

	FRANCHISE_NOT_FOUND(BusinessErrorType.NOT_FOUND, "La franquicia '%s' no existe"),
	FRANCHISE_ALREADY_EXISTS(BusinessErrorType.ALREADY_EXISTS, "La franquicia '%s' ya existe"),
	BRANCH_NOT_FOUND(BusinessErrorType.NOT_FOUND, "La sucursal '%s' no existe"),
	PRODUCT_NOT_FOUND(BusinessErrorType.NOT_FOUND, "El producto '%s' no existe"),
	PRODUCT_ALREADY_EXISTS(BusinessErrorType.ALREADY_EXISTS, "El producto '%s' ya existe en la sucursal"),
	INVALID_STOCK(BusinessErrorType.INVALID, "El stock no puede ser negativo: %s"),
	INVALID_NAME(BusinessErrorType.INVALID, "El nombre es obligatorio");

	private final BusinessErrorType type;
	private final String messageTemplate;

	BusinessError(BusinessErrorType type, String messageTemplate) {
		this.type = type;
		this.messageTemplate = messageTemplate;
	}

	public BusinessErrorType type() {
		return type;
	}

	public String message(Object... args) {
		return messageTemplate.formatted(args);
	}

}
