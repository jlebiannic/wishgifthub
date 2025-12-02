package com.wishgifthub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation de validation pour vérifier qu'une URL d'image est valide.
 * L'URL doit utiliser le protocole HTTP ou HTTPS et pointer vers un fichier image
 * (jpg, jpeg, png, gif, webp, svg).
 * S'applique aux types String et URI.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ImageUrlValidator.class, ImageUriValidator.class})
@Documented
public @interface ValidImageUrl {

    String message() default "L'URL d'image n'est pas valide (doit être une URL http(s) pointant vers une image)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

