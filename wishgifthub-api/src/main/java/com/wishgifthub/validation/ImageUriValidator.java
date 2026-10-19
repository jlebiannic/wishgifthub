package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

/**
 * Valide une URI d'image HTTP(S), sans imposer d'extension de fichier.
 */
public class ImageUriValidator extends AbstractUrlValidator implements ConstraintValidator<ValidImageUrl, URI> {
    @Override
    public boolean isValid(URI value, ConstraintValidatorContext context) {
        return validateUriWithLength(value, context);
    }
}
