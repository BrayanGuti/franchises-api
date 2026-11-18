package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.dto;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Franquicia")
public record FranchiseResponse(
		@Schema(description = "Identificador generado por el sistema", example = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f")
		String id,
		@Schema(description = "Nombre de la franquicia", example = "Cafeterías del Norte")
		String name) {

	public static FranchiseResponse from(Franchise franchise) {
		return new FranchiseResponse(franchise.id(), franchise.name());
	}

}
