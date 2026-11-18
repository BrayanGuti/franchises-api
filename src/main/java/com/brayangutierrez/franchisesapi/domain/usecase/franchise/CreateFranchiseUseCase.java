package com.brayangutierrez.franchisesapi.domain.usecase.franchise;

import java.util.UUID;

import com.brayangutierrez.franchisesapi.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.domain.model.franchise.gateways.FranchiseRepository;
import com.brayangutierrez.franchisesapi.domain.usecase.common.NameRules;

import reactor.core.publisher.Mono;

/**
 * Registra una franquicia nueva con un ID generado por el sistema.
 * <p>
 * Valida el nombre aunque el entry point ya lo haga: el dominio no confía en que todas las
 * entradas pasen por la API.
 */
public class CreateFranchiseUseCase {

	private final FranchiseRepository franchiseRepository;

	public CreateFranchiseUseCase(FranchiseRepository franchiseRepository) {
		this.franchiseRepository = franchiseRepository;
	}

	public Mono<Franchise> execute(String name) {
		return NameRules.normalize(name)
				.map(validName -> new Franchise(UUID.randomUUID().toString(), validName))
				.flatMap(franchiseRepository::save);
	}

}
