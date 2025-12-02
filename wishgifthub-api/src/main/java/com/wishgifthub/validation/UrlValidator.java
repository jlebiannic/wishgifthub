package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

/**
 * Validateur pour l'annotation {@link ValidUrl}.
 * Vérifie que l'URL est bien formée et utilise le protocole HTTP ou HTTPS.
 */
public class UrlValidator implements ConstraintValidator<ValidUrl, String> {

    @Override
    public void initialize(ValidUrl constraintAnnotation) {
        // Aucune initialisation nécessaire
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null ou vide est considéré comme valide (utiliser @NotNull/@NotBlank pour forcer la présence)
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();

            // Vérifier que le schéma existe
            if (scheme == null) {
                return false;
            }

            // Vérifier que le protocole est http ou https
            String schemeLower = scheme.toLowerCase();
            if (!"http".equals(schemeLower) && !"https".equals(schemeLower)) {
                return false;
            }

            // Vérifier que l'URL a un hôte
            return uri.getHost() != null && !uri.getHost().isEmpty();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

