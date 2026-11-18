package com.brayangutierrez.franchisesapi.domain.model.branch.gateways;

import com.brayangutierrez.franchisesapi.domain.model.branch.Branch;

import reactor.core.publisher.Mono;

/**
 * Persistencia de sucursales. La implementación vive en la infraestructura.
 */
public interface BranchRepository {

	/**
	 * Guarda una sucursal nueva dentro de su franquicia, de forma atómica: falla con
	 * {@code FRANCHISE_NOT_FOUND} si la franquicia no existe (sin dejar la sucursal huérfana)
	 * y con {@code BRANCH_ALREADY_EXISTS} si el ID ya existe.
	 */
	Mono<Branch> save(Branch branch);

}
