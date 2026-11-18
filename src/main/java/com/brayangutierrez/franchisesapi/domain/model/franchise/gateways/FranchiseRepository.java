package com.brayangutierrez.franchisesapi.domain.model.franchise.gateways;

import com.brayangutierrez.franchisesapi.domain.model.franchise.Franchise;

import reactor.core.publisher.Mono;

/**
 * Persistencia de franquicias. La implementación vive en la infraestructura.
 */
public interface FranchiseRepository {

	/**
	 * Guarda una franquicia nueva. Falla con {@code FRANCHISE_ALREADY_EXISTS} si el ID ya existe:
	 * nunca sobrescribe una franquicia.
	 */
	Mono<Franchise> save(Franchise franchise);

	/** Busca una franquicia por ID; vacío si no existe. */
	Mono<Franchise> findById(String id);

}
