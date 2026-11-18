package com.brayangutierrez.franchisesapi.integrations.dynamodb.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;

// Propiedades de conexión a DynamoDB, cargadas desde application.yaml.
// createTable: crea la tabla al arrancar (solo en local; en AWS la crea Terraform).
@ConfigurationProperties(prefix = "app.dynamodb")
public record DynamoDbProperties(
		URI endpoint,
		@NotBlank String region,
		@NotBlank String tableName,
		boolean createTable) {
}
