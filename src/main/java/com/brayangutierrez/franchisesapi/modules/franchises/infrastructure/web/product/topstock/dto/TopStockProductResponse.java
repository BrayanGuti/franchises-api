package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.topstock.dto;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.TopStockProduct;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Producto con más stock de una sucursal")
public record TopStockProductResponse(
		@Schema(description = "ID de la sucursal", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
		String branchId,
		@Schema(description = "Nombre de la sucursal", example = "Sucursal Centro")
		String branchName,
		@Schema(description = "ID del producto", example = "9b2e4c6a-1f3d-4e5b-8a7c-0d1e2f3a4b5c")
		String productId,
		@Schema(description = "Nombre del producto", example = "Café americano")
		String productName,
		@Schema(description = "Stock del producto", example = "50")
		int stock) {

	public static TopStockProductResponse from(TopStockProduct top) {
		return new TopStockProductResponse(top.branch().id(), top.branch().name(), top.product().id(),
				top.product().name(), top.product().stock());
	}

}
