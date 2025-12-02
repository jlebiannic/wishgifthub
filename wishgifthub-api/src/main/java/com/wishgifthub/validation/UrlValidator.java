package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validateur pour l'annotation {@link ValidUrl} appliquée aux String.
 * Vérifie que l'URL est bien formée, utilise le protocole HTTP ou HTTPS et ne dépasse pas 2048 caractères.
 */
public class UrlValidator extends AbstractUrlValidator implements ConstraintValidator<ValidUrl, String> {

    @Override
    public void initialize(ValidUrl constraintAnnotation) {
        // Aucune initialisation nécessaire
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return validateUrlString(value, context);
    }
}

