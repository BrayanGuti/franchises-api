package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RenameProductUseCaseTest {

	@Mock
	private ProductRepository repository;

	@InjectMocks
	private RenameProductUseCase useCase;

	@Test
	void renamesWithTheStrippedName() {
		Product renamed = new Product("p-1", "f-1", "b-1", "Nuevo nombre", 5);
		when(repository.rename("f-1", "b-1", "p-1", "Nuevo nombre")).thenReturn(Mono.just(renamed));

		StepVerifier.create(useCase.execute("f-1", "b-1", "p-1", "  Nuevo nombre  "))
				.expectNext(renamed)
				.verifyComplete();
	}

	@Test
	void rejectsBlankNameWithoutTouchingTheRepository() {
		StepVerifier.create(useCase.execute("f-1", "b-1", "p-1", "   "))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.INVALID_NAME))
				.verify();

		verify(repository, never()).rename(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void propagatesNotFound() {
		BusinessException notFound = new BusinessException(FranchiseError.PRODUCT_NOT_FOUND, "x");
		when(repository.rename("f-1", "b-1", "p-1", "Nuevo")).thenReturn(Mono.error(notFound));

		StepVerifier.create(useCase.execute("f-1", "b-1", "p-1", "Nuevo"))
				.expectErrorMatches(notFound::equals)
				.verify();
	}

}
