package com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.franchise.createfranchise.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear una franquicia")
public record CreateFranchiseRequest(
		@Schema(description = "Nombre de la franquicia", example = "Cafeterías del Norte")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String name) {
}
