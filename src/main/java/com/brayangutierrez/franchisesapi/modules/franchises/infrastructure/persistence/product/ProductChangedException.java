package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

/**
 * El producto cambió entre la lectura y la escritura condicionada: hay que volver a leerlo y
 * reintentar. Es interna de la persistencia; nunca llega al dominio.
 */
final class ProductChangedException extends RuntimeException {

	ProductChangedException(Throwable cause) {
		super(cause);
	}

}
