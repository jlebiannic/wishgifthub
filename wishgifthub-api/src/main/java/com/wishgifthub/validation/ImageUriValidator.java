package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Validateur pour l'annotation {@link ValidImageUrl} appliquée aux URI.
 * Vérifie que l'URI utilise le protocole HTTP ou HTTPS, ne dépasse pas 2048 caractères
 * et pointe vers un fichier image valide.
 */
public class ImageUriValidator extends AbstractUrlValidator implements ConstraintValidator<ValidImageUrl, URI> {

    private static final List<String> VALID_IMAGE_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp", ".ico"
    );

    @Override
    public void initialize(ValidImageUrl constraintAnnotation) {
        // Aucune initialisation nécessaire
    }

    @Override
    public boolean isValid(URI value, ConstraintValidatorContext context) {
        // Valider d'abord l'URI de base (protocole, longueur, etc.)
        if (!validateUriWithLength(value, context)) {
            return false;
        }

        // Si l'URI est null, c'est valide (déjà géré par validateUriWithLength)
        if (value == null) {
            return true;
        }

        // Vérifier que l'URL pointe vers un fichier image
        return validateImageExtension(value, context);
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

