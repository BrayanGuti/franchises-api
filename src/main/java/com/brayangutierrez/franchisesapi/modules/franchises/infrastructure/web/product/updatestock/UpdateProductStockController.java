package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.updatestock;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.core.web.validation.ValidId;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.UpdateProductStockUseCase;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto.ProductResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.updatestock.dto.UpdateStockRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Productos", description = "Gestión de productos de una sucursal")
public class UpdateProductStockController {

	private final UpdateProductStockUseCase updateProductStockUseCase;

	public UpdateProductStockController(UpdateProductStockUseCase updateProductStockUseCase) {
		this.updateProductStockUseCase = updateProductStockUseCase;
	}

	@PatchMapping("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}/stock")
	@Operation(summary = "Modificar el stock de un producto",
			description = "Reemplaza el stock por el valor indicado. Solo cambia el stock; el nombre no se toca.")
	@ApiResponse(responseCode = "200", description = "Stock actualizado; devuelve el producto")
	@ApiResponse(responseCode = "400", description = "Stock o identificadores inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "El producto no existe en esa sucursal",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<ProductResponse> updateStock(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId,
			@Parameter(description = "ID de la sucursal") @PathVariable @ValidId String branchId,
			@Parameter(description = "ID del producto") @PathVariable @ValidId String productId,
			@Valid @RequestBody UpdateStockRequest request) {
		return updateProductStockUseCase.execute(franchiseId, branchId, productId, request.stock())
				.map(ProductResponse::from);
	}

}
