package com.brayangutierrez.franchisesapi.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "com.brayangutierrez.franchisesapi.domain.usecase", includeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$"), useDefaultFilters = false)
public class UseCasesConfig {
}
