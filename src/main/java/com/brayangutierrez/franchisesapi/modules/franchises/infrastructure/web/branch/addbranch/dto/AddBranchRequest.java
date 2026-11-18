package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.addbranch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para agregar una sucursal a una franquicia")
public record AddBranchRequest(
		@Schema(description = "Nombre de la sucursal", example = "Sucursal Centro")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String name) {
}
