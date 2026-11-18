package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.removeproduct;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.core.web.validation.ValidId;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.RemoveProductUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Productos", description = "Gestión de productos de una sucursal")
public class RemoveProductController {

	private final RemoveProductUseCase removeProductUseCase;

	public RemoveProductController(RemoveProductUseCase removeProductUseCase) {
		this.removeProductUseCase = removeProductUseCase;
	}

	@DeleteMapping("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Eliminar un producto de una sucursal",
			description = "Elimina el producto y libera su nombre para que pueda usarse en un producto nuevo.")
	@ApiResponse(responseCode = "204", description = "Producto eliminado")
	@ApiResponse(responseCode = "400", description = "Identificadores inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "El producto no existe en esa sucursal",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "El producto cambió mientras se eliminaba; se puede reintentar",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<Void> removeProduct(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId,
			@Parameter(description = "ID de la sucursal") @PathVariable @ValidId String branchId,
			@Parameter(description = "ID del producto") @PathVariable @ValidId String productId) {
		return removeProductUseCase.execute(franchiseId, branchId, productId);
	}

}
