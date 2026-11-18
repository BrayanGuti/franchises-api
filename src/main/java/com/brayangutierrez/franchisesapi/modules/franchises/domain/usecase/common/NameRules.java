package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.common;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;

import reactor.core.publisher.Mono;

/**
 * Regla común para los nombres de franquicias, sucursales y productos.
 */
public final class NameRules {

	private NameRules() {
	}

	/**
	 * Quita los espacios de los extremos y exige que quede algo.
	 *
	 * @return el nombre normalizado, o error {@code INVALID_NAME} si es nulo o vacío
	 */
	public static Mono<String> normalize(String name) {
		return Mono.justOrEmpty(name)
				.map(String::strip)
				.filter(stripped -> !stripped.isEmpty())
				.switchIfEmpty(Mono.error(() -> new BusinessException(FranchiseError.INVALID_NAME)));
	}

}
