package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.renamefranchise;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.core.web.validation.ValidId;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.franchise.RenameFranchiseUseCase;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.dto.FranchiseResponse;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.renamefranchise.dto.RenameFranchiseRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Franquicias", description = "Gestión de franquicias")
public class RenameFranchiseController {

	private final RenameFranchiseUseCase renameFranchiseUseCase;

	public RenameFranchiseController(RenameFranchiseUseCase renameFranchiseUseCase) {
		this.renameFranchiseUseCase = renameFranchiseUseCase;
	}

	@PatchMapping("/api/franchises/{franchiseId}")
	@Operation(summary = "Actualizar el nombre de una franquicia")
	@ApiResponse(responseCode = "200", description = "Nombre actualizado; devuelve la franquicia")
	@ApiResponse(responseCode = "400", description = "Nombre o identificador inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "La franquicia no existe",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<FranchiseResponse> renameFranchise(
			@Parameter(description = "ID de la franquicia") @PathVariable @ValidId String franchiseId,
			@Valid @RequestBody RenameFranchiseRequest request) {
		return renameFranchiseUseCase.execute(franchiseId, request.name())
				.map(FranchiseResponse::from);
	}

}
