package com.brayangutierrez.franchisesapi.domain.usecase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.brayangutierrez.franchisesapi.domain.model.exception.BusinessError;
import com.brayangutierrez.franchisesapi.domain.model.exception.BusinessException;

import reactor.test.StepVerifier;

class NameRulesTest {

	@Test
	void stripsSurroundingSpaces() {
		StepVerifier.create(NameRules.normalize("  Sucursal Centro  "))
				.expectNext("Sucursal Centro")
				.verifyComplete();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   ", "\t\n" })
	void rejectsMissingNames(String name) {
		StepVerifier.create(NameRules.normalize(name))
				.expectErrorSatisfies(error -> assertThat(error)
						.isInstanceOfSatisfying(BusinessException.class,
								e -> assertThat(e.error()).isEqualTo(BusinessError.INVALID_NAME)))
				.verify();
	}

}
