package com.brayangutierrez.franchisesapi.domain.usecase.franchise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brayangutierrez.franchisesapi.domain.model.exception.BusinessError;
import com.brayangutierrez.franchisesapi.domain.model.exception.BusinessException;
import com.brayangutierrez.franchisesapi.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.domain.model.franchise.gateways.FranchiseRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateFranchiseUseCaseTest {

	@Mock
	private FranchiseRepository franchiseRepository;

	@InjectMocks
	private CreateFranchiseUseCase useCase;

	@Test
	void savesFranchiseWithGeneratedIdAndStrippedName() {
		when(franchiseRepository.save(any())).thenAnswer(call -> Mono.just(call.getArgument(0)));

		StepVerifier.create(useCase.execute("  Cafeterías del Norte  "))
				.assertNext(franchise -> {
					assertThat(franchise.name()).isEqualTo("Cafeterías del Norte");
					assertThat(UUID.fromString(franchise.id())).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void everyFranchiseGetsADifferentId() {
		when(franchiseRepository.save(any())).thenAnswer(call -> Mono.just(call.getArgument(0)));

		Franchise first = useCase.execute("Uno").block();
		Franchise second = useCase.execute("Dos").block();

		assertThat(first.id()).isNotEqualTo(second.id());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void rejectsMissingNameWithoutTouchingTheRepository(String name) {
		StepVerifier.create(useCase.execute(name))
				.expectErrorSatisfies(error -> assertThat(error)
						.isInstanceOfSatisfying(BusinessException.class,
								e -> assertThat(e.error()).isEqualTo(BusinessError.INVALID_NAME)))
				.verify();

		verify(franchiseRepository, never()).save(any());
	}

	@Test
	void propagatesRepositoryErrors() {
		BusinessException conflict = new BusinessException(BusinessError.FRANCHISE_ALREADY_EXISTS, "x");
		when(franchiseRepository.save(any())).thenReturn(Mono.error(conflict));

		StepVerifier.create(useCase.execute("Cafeterías del Norte"))
				.expectErrorMatches(conflict::equals)
				.verify();
	}

}
