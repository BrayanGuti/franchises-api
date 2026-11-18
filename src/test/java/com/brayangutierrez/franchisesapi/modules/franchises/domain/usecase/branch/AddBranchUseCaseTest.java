package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AddBranchUseCaseTest {

	private static final String FRANCHISE_ID = "3f2b8c1e-5d4a-4e7b-9c0d-1a2b3c4d5e6f";

	@Mock
	private BranchRepository branchRepository;

	@InjectMocks
	private AddBranchUseCase useCase;

	@Test
	void savesBranchInTheFranchiseWithGeneratedId() {
		when(branchRepository.save(any())).thenAnswer(call -> Mono.just(call.getArgument(0)));

		StepVerifier.create(useCase.execute(FRANCHISE_ID, "  Sucursal Centro "))
				.assertNext(branch -> {
					assertThat(branch.franchiseId()).isEqualTo(FRANCHISE_ID);
					assertThat(branch.name()).isEqualTo("Sucursal Centro");
					assertThat(UUID.fromString(branch.id())).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void rejectsBlankNameWithoutTouchingTheRepository() {
		StepVerifier.create(useCase.execute(FRANCHISE_ID, "  "))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.INVALID_NAME))
				.verify();

		verify(branchRepository, never()).save(any());
	}

	@Test
	void propagatesFranchiseNotFound() {
		BusinessException notFound = new BusinessException(FranchiseError.FRANCHISE_NOT_FOUND, FRANCHISE_ID);
		when(branchRepository.save(any())).thenReturn(Mono.error(notFound));

		StepVerifier.create(useCase.execute(FRANCHISE_ID, "Sucursal Centro"))
				.expectErrorMatches(notFound::equals)
				.verify();
	}

}
