package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch;

import java.util.UUID;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.common.NameRules;

import reactor.core.publisher.Mono;

/**
 * Agrega una sucursal a una franquicia existente, con un ID generado por el sistema.
 * <p>
 * La existencia de la franquicia la garantiza el repositorio en la misma escritura atómica:
 * consultarla antes y escribir después dejaría una ventana para condiciones de carrera.
 */
public class AddBranchUseCase {

	private final BranchRepository branchRepository;

	public AddBranchUseCase(BranchRepository branchRepository) {
		this.branchRepository = branchRepository;
	}

	public Mono<Branch> execute(String franchiseId, String name) {
		return NameRules.normalize(name)
				.map(validName -> new Branch(UUID.randomUUID().toString(), franchiseId, validName))
				.flatMap(branchRepository::save);
	}

}
