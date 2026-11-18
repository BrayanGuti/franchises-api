package com.brayangutierrez.franchisesapi.core.domain.exception;

/**
 * Contrato que cumple el catálogo de errores de cada módulo de negocio (por ejemplo, un enum).
 * El manejo de errores solo conoce este contrato, nunca los errores concretos de un módulo.
 */
public interface BusinessErrorCode {

	/** Categoría del error; el entry point la traduce al código HTTP. */
	BusinessErrorType type();

	/** Mensaje para el cliente, con los parámetros reemplazados. */
	String message(Object... args);

}
