package com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;

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

	/**
	 * Cambia solo el nombre de una sucursal existente y activa. Falla con {@code BRANCH_NOT_FOUND}
	 * si no existe (o se está eliminando), sin crearla.
	 */
	Mono<Branch> rename(String franchiseId, String branchId, String name);

	/**
	 * Elimina una sucursal con todos sus productos. Desde que empieza, la sucursal deja de aceptar
	 * productos nuevos o renombrados; las escrituras ya confirmadas se eliminan con ella, así que
	 * no quedan datos huérfanos. Es idempotente: si se interrumpe, repetirla la completa.
	 * Falla con {@code BRANCH_NOT_FOUND} si la sucursal no existe.
	 */
	Mono<Void> delete(String franchiseId, String branchId);

}
