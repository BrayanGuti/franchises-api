package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.renamebranch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nuevo nombre de la sucursal")
public record RenameBranchRequest(
		@Schema(description = "Nuevo nombre", example = "Sucursal Norte")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String name) {
}
