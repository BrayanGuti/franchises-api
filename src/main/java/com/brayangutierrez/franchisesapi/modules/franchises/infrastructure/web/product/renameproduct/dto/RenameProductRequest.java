package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.renameproduct.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nuevo nombre del producto, único dentro de su sucursal")
public record RenameProductRequest(
		@Schema(description = "Nuevo nombre", example = "Café espresso")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String name) {
}
