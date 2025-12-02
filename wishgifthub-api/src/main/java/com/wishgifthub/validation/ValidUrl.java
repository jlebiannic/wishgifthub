package com.wishgifthub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation de validation pour vérifier qu'une URL est valide.
 * L'URL doit utiliser le protocole HTTP ou HTTPS.
 * S'applique aux types String et URI.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {UrlValidator.class, UriValidator.class})
@Documented
public @interface ValidUrl {

    String message() default "L'URL n'est pas valide (doit commencer par http:// ou https://)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

