package com.brayangutierrez.franchisesapi.app;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

// Registra como beans los *UseCase de todos los módulos de negocio, sin anotaciones de Spring en el dominio.
@Configuration
@ComponentScan(basePackages = "com.brayangutierrez.franchisesapi.modules", includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$"), useDefaultFilters = false)
public class UseCasesConfig {
}
