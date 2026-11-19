package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;

import reactor.core.publisher.Mono;

/**
 * Elimina una sucursal y todos sus productos sin dejar datos huérfanos, aunque lleguen
 * peticiones sobre sus productos mientras se elimina.
 */
public class RemoveBranchUseCase {

	private final BranchRepository branchRepository;

	public RemoveBranchUseCase(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

	public Mono<Void> execute(String franchiseId, String branchId) {
		return branchRepository.delete(franchiseId, branchId);
	}

}
