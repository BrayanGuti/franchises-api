package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.branch;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.gateways.BranchRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RemoveBranchUseCaseTest {

	@Mock
	private BranchRepository branchRepository;

	@InjectMocks
	private RemoveBranchUseCase useCase;

	@Test
	void deletesTheBranch() {
		when(branchRepository.delete("f-1", "b-1")).thenReturn(Mono.empty());

		StepVerifier.create(useCase.execute("f-1", "b-1")).verifyComplete();
	}

	@Test
	void propagatesBranchNotFound() {
		BusinessException notFound = new BusinessException(FranchiseError.BRANCH_NOT_FOUND, "b-1");
		when(branchRepository.delete("f-1", "b-1")).thenReturn(Mono.error(notFound));

		StepVerifier.create(useCase.execute("f-1", "b-1"))
				.expectErrorMatches(notFound::equals)
				.verify();
	}

}
