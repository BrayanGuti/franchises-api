package com.brayangutierrez.franchisesapi.core.web.error;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;

/**
 * Traduce las excepciones a respuestas {@link ProblemDetail}, para que
 * todos los errores de la API tengan el mismo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	static final String VALIDATION_TITLE = "Solicitud inválida";
	static final String VALIDATION_DETAIL = "La solicitud contiene datos inválidos";
	static final String MALFORMED_DETAIL = "El cuerpo o los parámetros de la solicitud no tienen un formato válido";
	static final String INTERNAL_DETAIL = "Ocurrió un error inesperado";
	static final String UNKNOWN_ROUTE_DETAIL = "La ruta solicitada no existe";

	static final String NOT_FOUND_TITLE = "Recurso no encontrado";

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** Errores de negocio: la categoría del error define el código HTTP. */
	@ExceptionHandler(BusinessException.class)
	public ProblemDetail handleBusiness(BusinessException ex, ServerWebExchange exchange) {
		return switch (ex.error().type()) {
			case NOT_FOUND -> problem(HttpStatus.NOT_FOUND, NOT_FOUND_TITLE, ex.getMessage(), exchange);
			case ALREADY_EXISTS -> problem(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage(), exchange);
			case INVALID -> problem(HttpStatus.BAD_REQUEST, VALIDATION_TITLE, ex.getMessage(), exchange);
		};
	}

	/**
	 * Cuerpo de la petición que no cumple las anotaciones de validación
	 * ({@code @Valid}).
	 */
	@ExceptionHandler(WebExchangeBindException.class)
	public ProblemDetail handleBodyValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
		Map<String, String> errors = new LinkedHashMap<>();
		addFieldErrors(errors, ex.getFieldErrors());
		return validationProblem(errors, exchange);
	}

	/**
	 * Parámetros que no cumplen sus restricciones (por ejemplo, un ID inválido en la ruta).
	 * Cuando un método tiene restricciones en sus parámetros, Spring valida también el cuerpo por
	 * esta vía: sus errores llegan como {@link ParameterErrors} y se reportan por campo, igual
	 * que en {@link #handleBodyValidation}.
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail handleParameterValidation(HandlerMethodValidationException ex, ServerWebExchange exchange) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getParameterValidationResults().forEach(result -> {
			if (result instanceof ParameterErrors bodyErrors) {
				addFieldErrors(errors, bodyErrors.getFieldErrors());
			} else {
				errors.merge(
						Objects.requireNonNullElse(result.getMethodParameter().getParameterName(), "parametro"),
						result.getResolvableErrors().stream()
								.map(GlobalExceptionHandler::messageOf)
								.collect(Collectors.joining("; ")),
						(first, second) -> first + "; " + second);
			}
		});
		return validationProblem(errors, exchange);
	}

	/** JSON mal formado, tipos incorrectos o parámetros obligatorios ausentes. */
	@ExceptionHandler(ServerWebInputException.class)
	public ProblemDetail handleMalformedInput(ServerWebInputException ex, ServerWebExchange exchange) {
		return problem(HttpStatus.BAD_REQUEST, VALIDATION_TITLE, MALFORMED_DETAIL, exchange);
	}

	/** Ruta que no corresponde a ningún endpoint. */
	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleUnknownRoute(NoResourceFoundException ex, ServerWebExchange exchange) {
		return problem(HttpStatus.NOT_FOUND, NOT_FOUND_TITLE, UNKNOWN_ROUTE_DETAIL, exchange);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ProblemDetail handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
		HttpStatusCode status = ex.getStatusCode();
		HttpStatus resolved = HttpStatus.resolve(status.value());
		String title = resolved != null ? resolved.getReasonPhrase() : "Error";
		return problem(status, title, ex.getReason(), exchange);
	}

	/** Último recurso: nunca expone detalles internos al cliente, pero los deja en el log. */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex, ServerWebExchange exchange) {
		log.error("Error no controlado en {}", exchange.getRequest().getPath(), ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", INTERNAL_DETAIL, exchange);
	}

	private static ProblemDetail problem(HttpStatusCode status, String title, String detail,
			ServerWebExchange exchange) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setInstance(URI.create(exchange.getRequest().getPath().value()));
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}

	private static ProblemDetail validationProblem(Map<String, String> errors, ServerWebExchange exchange) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_TITLE, VALIDATION_DETAIL, exchange);
		problem.setProperty("errors", errors);
		return problem;
	}

	/** Un mensaje por campo; si un campo tiene varios errores, se conserva el primero. */
	private static void addFieldErrors(Map<String, String> errors, List<FieldError> fieldErrors) {
		fieldErrors.forEach(error -> errors.putIfAbsent(error.getField(), messageOf(error)));
	}

	private static String messageOf(MessageSourceResolvable error) {
		return Objects.requireNonNullElse(error.getDefaultMessage(), "Valor inválido");
	}

}
