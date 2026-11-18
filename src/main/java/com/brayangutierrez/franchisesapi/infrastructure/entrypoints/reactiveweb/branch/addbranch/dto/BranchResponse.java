package com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.branch.addbranch.dto;

import com.brayangutierrez.franchisesapi.domain.model.branch.Branch;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sucursal")
public record BranchResponse(
		@Schema(description = "Identificador generado por el sistema", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
		String id,
		@Schema(description = "Franquicia a la que pertenece", example = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f")
		String franchiseId,
		@Schema(description = "Nombre de la sucursal", example = "Sucursal Centro")
		String name) {

	public static BranchResponse from(Branch branch) {
		return new BranchResponse(branch.id(), branch.franchiseId(), branch.name());
	}

}
