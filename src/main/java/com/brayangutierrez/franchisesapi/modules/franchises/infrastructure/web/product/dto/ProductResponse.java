package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producto")
public record ProductResponse(
		@Schema(description = "Identificador generado por el sistema", example = "9b2e4c6a-1f3d-4e5b-8a7c-0d1e2f3a4b5c")
		String id,
		@Schema(description = "Franquicia a la que pertenece", example = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f")
		String franchiseId,
		@Schema(description = "Sucursal a la que pertenece", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
		String branchId,
		@Schema(description = "Nombre del producto", example = "Café americano")
		String name,
		@Schema(description = "Cantidad en stock", example = "25")
		int stock) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(product.id(), product.franchiseId(), product.branchId(), product.name(),
				product.stock());
	}

}
