package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.renamefranchise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nuevo nombre de la franquicia")
public record RenameFranchiseRequest(
		@Schema(description = "Nuevo nombre", example = "Cafeterías del Caribe")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String name) {
}
