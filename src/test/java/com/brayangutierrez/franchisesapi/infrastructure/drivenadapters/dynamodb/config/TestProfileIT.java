package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Garantiza que las pruebas de integración siempre usen el perfil de prueba.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class TestProfileIT {

	@Autowired
	private Environment environment;

	@Autowired
	private DynamoDbProperties properties;

	@Test
	void integrationTestsRunWithTestProfile() {
		assertThat(environment.getActiveProfiles()).containsExactly("test");
	}

	@Test
	void integrationTestsUseTestDatabase() {
		assertThat(properties.endpoint()).isEqualTo(URI.create("http://localhost:8001"));
	}

}
