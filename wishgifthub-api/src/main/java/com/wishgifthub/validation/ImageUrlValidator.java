package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valide une URL d'image HTTP(S), sans imposer d'extension de fichier.
 */
public class ImageUrlValidator extends AbstractUrlValidator implements ConstraintValidator<ValidImageUrl, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return validateUrlString(value, context);
    }
}
