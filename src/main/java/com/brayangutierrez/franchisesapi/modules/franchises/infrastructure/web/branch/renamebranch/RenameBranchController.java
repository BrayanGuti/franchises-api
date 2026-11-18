package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.renamebranch;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.core.web.validation.ValidId;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch.RenameBranchUseCase;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.branch.renamebranch.dto.RenameBranchRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Sucursales", description = "Gestión de sucursales de una franquicia")
public class RenameBranchController {

	private final RenameBranchUseCase renameBranchUseCase;

	public RenameBranchController(RenameBranchUseCase renameBranchUseCase) {
		this.renameBranchUseCase = renameBranchUseCase;
	}

	@PatchMapping("/api/franchises/{franchiseId}/branches/{branchId}")
	@Operation(summary = "Actualizar el nombre de una sucursal")
	@ApiResponse(responseCode = "200", description = "Nombre actualizado; devuelve la sucursal")
	@ApiResponse(responseCode = "400", description = "Nombre o identificadores inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "La sucursal no existe",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<BranchResponse> renameBranch(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId,
			@Parameter(description = "ID de la sucursal") @PathVariable @ValidId String branchId,
			@Valid @RequestBody RenameBranchRequest request) {
		return renameBranchUseCase.execute(franchiseId, branchId, request.name())
				.map(BranchResponse::from);
	}

}
