package com.brayangutierrez.franchisesapi.integrations.dynamodb.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

class MigrationStatusTest {

	private final MigrationStatus status = new MigrationStatus();

	@Test
	void publishesNothingBeforeMigrating() {
		assertThat(contribution()).isEmpty();
	}

	@Test
	void publishesNothingWhenThereAreNoMigrations() {
		status.update(new MigrationResult(List.of(), null));

		assertThat(contribution()).isEmpty();
	}

	@Test
	void publishesTheVerifiedVersionAndWhatWasAppliedOnStartup() {
		status.update(new MigrationResult(List.of("V002"), "V002"));

		assertThat(contribution()).containsEntry("migrations",
				Map.of("version", "V002", "appliedOnStartup", List.of("V002")));
	}

	private Map<String, Object> contribution() {
		Info.Builder builder = new Info.Builder();
		status.contribute(builder);
		return builder.build().getDetails();
	}

}
