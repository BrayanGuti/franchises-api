package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;

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
								e -> assertThat(e.error()).isEqualTo(FranchiseError.INVALID_NAME)))
				.verify();
	}

}
