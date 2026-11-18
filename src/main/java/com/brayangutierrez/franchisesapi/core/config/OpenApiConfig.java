package com.brayangutierrez.franchisesapi.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Metadatos de la documentación OpenAPI que se publica en /v3/api-docs y
 * /swagger-ui.html.
 */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI franchisesOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Franchises API")
				.version("v1")
				.description("API reactiva para gestionar franquicias, sus sucursales y los productos de cada "
						+ "sucursal."));
	}

}
