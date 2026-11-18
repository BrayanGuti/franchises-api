package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.addproduct.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para agregar un producto a una sucursal")
public record AddProductRequest(
		@Schema(description = "Nombre del producto, único dentro de la sucursal", example = "Café americano")
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
		String name,
		@Schema(description = "Cantidad en stock", example = "25", minimum = "0")
		@NotNull(message = "El stock es obligatorio")
		@PositiveOrZero(message = "El stock no puede ser negativo")
		Integer stock) {
}
