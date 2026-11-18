package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.renameproduct;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.core.web.validation.ValidId;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.RenameProductUseCase;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto.ProductResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.renameproduct.dto.RenameProductRequest;

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
public class RenameProductController {

	private final RenameProductUseCase renameProductUseCase;

	public RenameProductController(RenameProductUseCase renameProductUseCase) {
		this.renameProductUseCase = renameProductUseCase;
	}

	@PatchMapping("/api/franchises/{franchiseId}/branches/{branchId}/products/{productId}")
	@Operation(summary = "Actualizar el nombre de un producto",
			description = "Cambia solo el nombre; el stock no se toca. El nombre nuevo debe ser único en la sucursal.")
	@ApiResponse(responseCode = "200", description = "Nombre actualizado; devuelve el producto")
	@ApiResponse(responseCode = "400", description = "Nombre o identificadores inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "El producto o la sucursal no existen",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Otro producto de la sucursal ya usa ese nombre, o el producto "
			+ "cambió mientras se renombraba",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<ProductResponse> renameProduct(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId,
			@Parameter(description = "ID de la sucursal") @PathVariable @ValidId String branchId,
			@Parameter(description = "ID del producto") @PathVariable @ValidId String productId,
			@Valid @RequestBody RenameProductRequest request) {
		return renameProductUseCase.execute(franchiseId, branchId, productId, request.name())
				.map(ProductResponse::from);
	}

}
