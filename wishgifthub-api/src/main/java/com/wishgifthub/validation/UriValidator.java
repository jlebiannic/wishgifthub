package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

/**
 * Validateur pour l'annotation {@link ValidUrl} appliquée aux URI.
 * Vérifie que l'URI utilise le protocole HTTP ou HTTPS.
 */
public class UriValidator implements ConstraintValidator<ValidUrl, URI> {

    @Override
    public void initialize(ValidUrl constraintAnnotation) {
        // Aucune initialisation nécessaire
    }

    @Override
    public boolean isValid(URI value, ConstraintValidatorContext context) {
        // null est considéré comme valide (utiliser @NotNull pour forcer la présence)
        if (value == null) {
            return true;
        }

        String scheme = value.getScheme();

        // Vérifier que le schéma existe
        if (scheme == null) {
            return false;
        }

        // Vérifier que le protocole est http ou https
        String schemeLower = scheme.toLowerCase();
        if (!"http".equals(schemeLower) && !"https".equals(schemeLower)) {
            return false;
        }

        // Vérifier que l'URI a un hôte
        return value.getHost() != null && !value.getHost().isEmpty();
    }
}

