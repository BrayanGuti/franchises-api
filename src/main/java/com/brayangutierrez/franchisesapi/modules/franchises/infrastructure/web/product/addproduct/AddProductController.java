package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.addproduct;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.core.web.validation.ValidId;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.AddProductUseCase;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.addproduct.dto.AddProductRequest;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.dto.ProductResponse;

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
public class AddProductController {

	private final AddProductUseCase addProductUseCase;

	public AddProductController(AddProductUseCase addProductUseCase) {
		this.addProductUseCase = addProductUseCase;
	}

	@PostMapping("/api/franchises/{franchiseId}/branches/{branchId}/products")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Agregar un producto a una sucursal",
			description = "Crea un producto con un ID generado por el sistema. El nombre es único dentro de la "
					+ "sucursal, sin distinguir mayúsculas, tildes ni espacios sobrantes.")
	@ApiResponse(responseCode = "201", description = "Producto creado")
	@ApiResponse(responseCode = "400", description = "Datos o identificadores inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "La sucursal no existe",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Ya existe un producto con ese nombre en la sucursal",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<ProductResponse> addProduct(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId,
			@Parameter(description = "ID de la sucursal") @PathVariable @ValidId String branchId,
			@Valid @RequestBody AddProductRequest request) {
		return addProductUseCase.execute(franchiseId, branchId, request.name(), request.stock())
				.map(ProductResponse::from);
	}

}
