package com.brayangutierrez.franchisesapi.integrations.dynamodb.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifica que cada perfil apunte a su propia instancia de DynamoDB Local,
 * leyendo los YAML directamente y sin levantar el contexto de Spring.
 */
class DynamoDbProfilesTest {

	@Test
	void devProfilePointsToDevDatabase() throws IOException {
		DynamoDbProperties properties = bindWithProfile("dev");

		assertThat(properties.endpoint()).isEqualTo(URI.create("http://localhost:8000"));
		assertThat(properties.region()).isEqualTo("us-east-1");
		assertThat(properties.tableName()).isEqualTo("franchises");
		assertThat(properties.createTable()).isTrue();
	}

	@Test
	void testProfilePointsToTestDatabase() throws IOException {
		DynamoDbProperties properties = bindWithProfile("test");

		assertThat(properties.endpoint()).isEqualTo(URI.create("http://localhost:8001"));
		assertThat(properties.region()).isEqualTo("us-east-1");
		assertThat(properties.tableName()).isEqualTo("franchises");
		assertThat(properties.createTable()).isTrue();
	}

	@Test
	void baseConfigurationTargetsAwsWithoutCreatingTable() throws IOException {
		StandardEnvironment environment = environmentWith("application.yaml");

		DynamoDbProperties properties = bind(environment);

		assertThat(properties.endpoint()).isNull();
		assertThat(properties.createTable()).isFalse();
	}

	@Test
	void prodProfileUsesAwsAndNeverCreatesTheTable() throws IOException {
		DynamoDbProperties properties = bindWithProfile("prod");

		assertThat(properties.endpoint()).isNull();
		assertThat(properties.createTable()).isFalse();
		assertThat(properties.tableName()).isEqualTo("franchises");
	}

	private DynamoDbProperties bindWithProfile(String profile) throws IOException {
		return bind(environmentWith("application-" + profile + ".yaml", "application.yaml"));
	}

	/**
	 * Simula el Environment que arma Spring Boot al arrancar con un perfil activo,
	 * sin levantar la aplicación. Si una propiedad se repite, gana el primer archivo.
	 */
	private StandardEnvironment environmentWith(String... files) throws IOException {
		StandardEnvironment environment = new StandardEnvironment();
		YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
		List<PropertySource<?>> sources = new ArrayList<>();
		for (String file : files) {
			sources.addAll(loader.load(file, new ClassPathResource(file)));
		}
		Collections.reverse(sources);
		sources.forEach(environment.getPropertySources()::addFirst);
		return environment;
	}

	private DynamoDbProperties bind(StandardEnvironment environment) {
		return new Binder(ConfigurationPropertySources.get(environment))
				.bindOrCreate("app.dynamodb", DynamoDbProperties.class);
	}

}
