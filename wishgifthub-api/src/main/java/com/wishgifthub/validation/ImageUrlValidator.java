package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Validateur pour l'annotation {@link ValidImageUrl} appliquée aux String.
 * Vérifie que l'URL utilise le protocole HTTP ou HTTPS, ne dépasse pas 2048 caractères
 * et pointe vers un fichier image valide.
 */
public class ImageUrlValidator extends AbstractUrlValidator implements ConstraintValidator<ValidImageUrl, String> {

    private static final List<String> VALID_IMAGE_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp", ".ico"
    );

    @Override
    public void initialize(ValidImageUrl constraintAnnotation) {
        // Aucune initialisation nécessaire
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Valider d'abord l'URL de base (protocole, longueur, etc.)
        if (!validateUrlString(value, context)) {
            return false;
        }

        // Si la valeur est null ou vide, c'est valide (déjà géré par validateUrlString)
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        // Vérifier que l'URL pointe vers un fichier image
        try {
            URI uri = URI.create(value);
            return validateImageExtension(uri, context);
        } catch (IllegalArgumentException e) {
            // Déjà géré par validateUrlString
            return false;
        }
    }

    /**
     * Vérifie que l'URI pointe vers un fichier image valide.
     *
     * @param uri l'URI à valider
     * @param context le contexte de validation
     * @return true si l'URI pointe vers une image, false sinon
     */
    private boolean validateImageExtension(URI uri, ConstraintValidatorContext context) {
        String path = uri.getPath();

        if (path == null || path.isEmpty()) {
            buildViolation(context, "L'URL de l'image doit pointer vers un fichier");
            return false;
        }

        // Extraire l'extension en ignorant les paramètres de requête
        String pathLower = path.toLowerCase();

        // Vérifier si le chemin se termine par une extension d'image valide
        boolean hasValidExtension = VALID_IMAGE_EXTENSIONS.stream()
            .anyMatch(pathLower::endsWith);

        if (!hasValidExtension) {
            buildViolation(context,
                "L'URL doit pointer vers une image valide (extensions acceptées : jpg, jpeg, png, gif, webp, svg, bmp, ico)");
            return false;
        }

        return true;
    }

    /**
     * Construit un message de violation personnalisé.
     *
     * @param context le contexte de validation
     * @param message le message d'erreur à afficher
     */
    private void buildViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
            .addConstraintViolation();
    }
}

