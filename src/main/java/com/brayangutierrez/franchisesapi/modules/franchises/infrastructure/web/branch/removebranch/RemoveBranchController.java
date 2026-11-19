package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.removebranch;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.core.web.validation.ValidId;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch.RemoveBranchUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Sucursales", description = "Gestión de sucursales de una franquicia")
public class RemoveBranchController {

	private final RemoveBranchUseCase removeBranchUseCase;

	public RemoveBranchController(RemoveBranchUseCase removeBranchUseCase) {
		this.removeBranchUseCase = removeBranchUseCase;
	}

	@DeleteMapping("/api/franchises/{franchiseId}/branches/{branchId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Eliminar una sucursal y sus productos",
			description = "Desde que empieza, la sucursal deja de aceptar productos nuevos o renombrados; los "
					+ "cambios ya confirmados se eliminan con ella. Si se interrumpe, repetir la petición la completa.")
	@ApiResponse(responseCode = "204", description = "Sucursal y productos eliminados")
	@ApiResponse(responseCode = "400", description = "Identificadores inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "La sucursal no existe",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "No terminó de eliminarse; se puede reintentar",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<Void> removeBranch(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId,
			@Parameter(description = "ID de la sucursal") @PathVariable @ValidId String branchId) {
		return removeBranchUseCase.execute(franchiseId, branchId);
	}

}
