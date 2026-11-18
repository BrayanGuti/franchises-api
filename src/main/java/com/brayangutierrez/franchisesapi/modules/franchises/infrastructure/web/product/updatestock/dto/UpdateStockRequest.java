package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.updatestock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Nuevo stock del producto (valor absoluto, no un incremento)")
public record UpdateStockRequest(
		@Schema(description = "Cantidad en stock", example = "40", minimum = "0")
		@NotNull(message = "El stock es obligatorio")
		@PositiveOrZero(message = "El stock no puede ser negativo")
		Integer stock) {
}
