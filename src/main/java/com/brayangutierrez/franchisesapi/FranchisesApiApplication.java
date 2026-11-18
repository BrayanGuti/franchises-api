package com.brayangutierrez.franchisesapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FranchisesApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FranchisesApiApplication.class, args);
	}

}
