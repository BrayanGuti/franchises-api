package com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.branch.addbranch;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.domain.usecase.branch.AddBranchUseCase;
import com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.branch.addbranch.dto.AddBranchRequest;
import com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.branch.addbranch.dto.BranchResponse;
import com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.validation.ValidId;

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
public class AddBranchController {

	private final AddBranchUseCase addBranchUseCase;

	public AddBranchController(AddBranchUseCase addBranchUseCase) {
		this.addBranchUseCase = addBranchUseCase;
	}

	@PostMapping("/api/franchises/{franchiseId}/branches")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Agregar una sucursal a una franquicia",
			description = "Crea una sucursal con un ID generado por el sistema dentro de una franquicia existente.")
	@ApiResponse(responseCode = "201", description = "Sucursal creada")
	@ApiResponse(responseCode = "400", description = "Datos o identificador inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "La franquicia no existe",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<BranchResponse> addBranch(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId,
			@Valid @RequestBody AddBranchRequest request) {
		return addBranchUseCase.execute(franchiseId, request.name())
				.map(BranchResponse::from);
	}

}
