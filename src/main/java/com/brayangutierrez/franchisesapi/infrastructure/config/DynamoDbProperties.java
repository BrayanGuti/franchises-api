package com.brayangutierrez.franchisesapi.infrastructure.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;

// Propiedades de conexión a DynamoDB, cargadas desde application.yml
@ConfigurationProperties(prefix = "app.dynamodb")
public record DynamoDbProperties(
		URI endpoint,
		@NotBlank String region,
		@NotBlank String tableName) {
}
