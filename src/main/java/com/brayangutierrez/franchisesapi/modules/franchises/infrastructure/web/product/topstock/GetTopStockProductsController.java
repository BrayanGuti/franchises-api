package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.topstock;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.core.web.validation.ValidId;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product.GetTopStockProductsUseCase;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.product.topstock.dto.TopStockProductResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Flux;

@RestController
@Tag(name = "Productos", description = "Gestión de productos de una sucursal")
public class GetTopStockProductsController {

	private final GetTopStockProductsUseCase getTopStockProductsUseCase;

	public GetTopStockProductsController(GetTopStockProductsUseCase getTopStockProductsUseCase) {
		this.getTopStockProductsUseCase = getTopStockProductsUseCase;
	}

	@GetMapping("/api/franchises/{franchiseId}/top-stock-products")
	@Operation(summary = "Producto con más stock por sucursal",
			description = "Para cada sucursal de la franquicia devuelve el producto con más stock e indica a qué "
					+ "sucursal pertenece. Si hay empate se devuelven todos los empatados; las sucursales sin "
					+ "productos se omiten. Orden: sucursal y luego producto, alfabéticamente.")
	@ApiResponse(responseCode = "200", description = "Productos con más stock (vacío si no hay productos)",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = TopStockProductResponse.class))))
	@ApiResponse(responseCode = "400", description = "Identificador inválido",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "La franquicia no existe",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Flux<TopStockProductResponse> getTopStockProducts(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId) {
		return getTopStockProductsUseCase.execute(franchiseId)
				.map(TopStockProductResponse::from);
	}

}
