package com.brayangutierrez.franchisesapi.infrastructure.entrypoints.reactiveweb.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

/**
 * Identificador con formato UUID, como los que genera el sistema. Un ID mal formado se rechaza
 * con 400 sin llegar a consultar la base de datos.
 */
@Documented
@Constraint(validatedBy = {})
@Pattern(regexp = ValidId.UUID_PATTERN)
@ReportAsSingleViolation
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidId {

	String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

	String message() default "El identificador debe ser un UUID válido";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
