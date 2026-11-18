package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.franchise;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.common.NameRules;

import reactor.core.publisher.Mono;

/**
 * Cambia el nombre de una franquicia existente.
 */
public class RenameFranchiseUseCase {

	private final FranchiseRepository franchiseRepository;

	public RenameFranchiseUseCase(FranchiseRepository franchiseRepository) {
		this.franchiseRepository = franchiseRepository;
	}

	public Mono<Franchise> execute(String franchiseId, String name) {
		return NameRules.normalize(name)
				.flatMap(validName -> franchiseRepository.rename(franchiseId, validName));
	}

}
