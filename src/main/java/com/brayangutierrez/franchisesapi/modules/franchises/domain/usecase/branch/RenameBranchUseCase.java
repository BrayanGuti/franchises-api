package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.common.NameRules;

import reactor.core.publisher.Mono;

/**
 * Cambia el nombre de una sucursal existente.
 */
public class RenameBranchUseCase {

	private final BranchRepository branchRepository;

	public RenameBranchUseCase(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

	public Mono<Branch> execute(String franchiseId, String branchId, String name) {
		return NameRules.normalize(name)
				.flatMap(validName -> branchRepository.rename(franchiseId, branchId, validName));
	}

}
