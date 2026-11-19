package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import reactor.core.publisher.Mono;

class MigrationVersionTest {

	@Test
	void versionAndDescriptionComeFromTheName() {
		MigrationVersion version = MigrationVersion.parse("V012__BackfillCategory");

		assertThat(version.number()).isEqualTo(12);
		assertThat(version.description()).isEqualTo("BackfillCategory");
		assertThat(version.label()).isEqualTo("V012");
	}

	@Test
	void byDefaultTheNameIsTheClassName() {
		Migration baseline = new V001__Sample();

		assertThat(MigrationVersion.of(baseline).label()).isEqualTo("V001");
	}

	@ParameterizedTest
	@ValueSource(strings = { "V1__Short", "V0001__TooLong", "V001_SingleUnderscore", "V001__", "Baseline",
			"v001__Lowercase", "V001__With-Dash" })
	void malformedNamesAreRejected(String name) {
		assertThatThrownBy(() -> MigrationVersion.parse(name))
				.isInstanceOf(MigrationException.class)
				.hasMessageContaining(name);
	}

	@Test
	void versionZeroIsRejected() {
		assertThatThrownBy(() -> MigrationVersion.parse("V000__Nothing"))
				.isInstanceOf(MigrationException.class)
				.hasMessageContaining("V001");
	}

	@Test
	void versionsAreOrderedByNumber() {
		assertThat(MigrationVersion.parse("V010__B")).isGreaterThan(MigrationVersion.parse("V002__A"));
	}

	@Test
	void runnerRejectsRepeatedVersionsBeforeTouchingTheDatabase() {
		List<Migration> migrations = List.of(noop("V002__First"), noop("V002__Second"));

		assertThatThrownBy(() -> new MigrationRunner(migrations, null, null, null))
				.isInstanceOf(MigrationException.class)
				.hasMessageContaining("V002__First")
				.hasMessageContaining("V002__Second");
	}

	@Test
	void runnerReportsTheLatestKnownVersion() {
		MigrationRunner runner = new MigrationRunner(List.of(noop("V003__C"), noop("V001__A")), null, null, null);

		assertThat(runner.latestVersion()).isEqualTo("V003");
		assertThat(new MigrationRunner(List.of(), null, null, null).latestVersion()).isNull();
	}

	private static Migration noop(String name) {
		return new TestMigration(name, Mono::empty);
	}

	private static final class V001__Sample implements Migration {

		@Override
		public Mono<Void> migrate() {
			return Mono.empty();
		}

	}

}
