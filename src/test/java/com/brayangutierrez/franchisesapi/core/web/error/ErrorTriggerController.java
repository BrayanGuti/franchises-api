package com.brayangutierrez.franchisesapi.core.web.error;

import java.net.URI;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import reactor.core.publisher.Mono;

@TestComponent
@RestController
@RequestMapping("/test")
class ErrorTriggerController {

	static WebTestClient client() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		return WebTestClient.bindToController(new ErrorTriggerController())
				.controllerAdvice(new GlobalExceptionHandler())
				.validator(validator)
				.build();
	}

	@GetMapping("/not-found")
	Mono<String> notFound() {
		return Mono.error(new BusinessException(TestError.RESOURCE_NOT_FOUND, "f-1"));
	}

	@GetMapping("/conflict")
	Mono<String> conflict() {
		return Mono.error(new BusinessException(TestError.RESOURCE_ALREADY_EXISTS, "p-1"));
	}

	@GetMapping("/invalid")
	Mono<String> invalid() {
		return Mono.error(new BusinessException(TestError.INVALID_VALUE, -1));
	}

	@PostMapping("/body")
	Mono<String> body(@Valid @RequestBody Payload payload) {
		return Mono.just("ok");
	}

	@GetMapping("/items/{id}")
	Mono<String> item(
			@PathVariable @Pattern(regexp = "[a-z0-9-]+", message = "El identificador no es válido") String id) {
		return Mono.just(id);
	}

	/** ID en la ruta + cuerpo: Spring valida ambos juntos (validación de método). */
	@PostMapping("/items/{id}")
	Mono<String> itemWithBody(
			@PathVariable @Pattern(regexp = "[a-z0-9-]+", message = "El identificador no es válido") String id,
			@Valid @RequestBody Payload payload) {
		return Mono.just(id);
	}

	@GetMapping("/numbers/{number}")
	Mono<Integer> number(@PathVariable int number) {
		return Mono.just(number);
	}

	@GetMapping("/unknown-route")
	Mono<String> unknownRoute() {
		return Mono.error(new NoResourceFoundException(URI.create("/test/unknown-route"), "unknown-route"));
	}

	@GetMapping("/forbidden")
	Mono<String> forbidden() {
		return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado"));
	}

	@GetMapping("/custom-status")
	Mono<String> customStatus() {
		return Mono.error(new ResponseStatusException(HttpStatusCode.valueOf(499)));
	}

	@GetMapping("/boom")
	Mono<String> boom() {
		return Mono.error(new IllegalStateException("detalle secreto de infraestructura"));
	}

	record Payload(
			@NotBlank(message = "El nombre es obligatorio") String name,
			@PositiveOrZero(message = "El stock no puede ser negativo") Integer stock) {
	}

}
