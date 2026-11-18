package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.createfranchise;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.franchise.CreateFranchiseUseCase;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.createfranchise.dto.CreateFranchiseRequest;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.web.franchise.dto.FranchiseResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/franchises")
@Tag(name = "Franquicias", description = "Gestión de franquicias")
public class CreateFranchiseController {

	private final CreateFranchiseUseCase createFranchiseUseCase;

	public CreateFranchiseController(CreateFranchiseUseCase createFranchiseUseCase) {
		this.createFranchiseUseCase = createFranchiseUseCase;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Agregar una franquicia", description = "Crea una franquicia con un ID generado por el sistema.")
	@ApiResponse(responseCode = "201", description = "Franquicia creada")
	@ApiResponse(responseCode = "400", description = "Datos inválidos",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	public Mono<FranchiseResponse> createFranchise(@Valid @RequestBody CreateFranchiseRequest request) {
		return createFranchiseUseCase.execute(request.name())
				.map(FranchiseResponse::from);
	}

}
