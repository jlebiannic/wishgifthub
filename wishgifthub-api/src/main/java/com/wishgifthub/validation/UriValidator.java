package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

/**
 * Validateur pour l'annotation {@link ValidUrl} appliquée aux URI.
 * Vérifie que l'URI utilise le protocole HTTP ou HTTPS et ne dépasse pas 2048 caractères.
 */
public class UriValidator extends AbstractUrlValidator implements ConstraintValidator<ValidUrl, URI> {

    @Override
    public void initialize(ValidUrl constraintAnnotation) {
        // Aucune initialisation nécessaire
    }

    @Override
    public boolean isValid(URI value, ConstraintValidatorContext context) {
        return validateUriWithLength(value, context);
    }
}

