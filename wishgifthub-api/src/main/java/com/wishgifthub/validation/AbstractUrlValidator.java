package com.wishgifthub.validation;

import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;

/**
 * Classe abstraite de base pour la validation des URLs.
 * Factorise la logique commune de validation entre UrlValidator et UriValidator.
 */
public abstract class AbstractUrlValidator {

    protected static final int MAX_URL_LENGTH = 2048;

    /**
     * Valide une URL sous forme de chaîne de caractères.
     * Vérifie : null/vide, longueur, format URI.
     *
     * @param urlString l'URL à valider
     * @param context le contexte de validation
     * @return true si l'URL est valide, false sinon
     */
    protected boolean validateUrlString(String urlString, ConstraintValidatorContext context) {
        // null ou vide est considéré comme valide (utiliser @NotNull/@NotBlank pour forcer la présence)
        if (urlString == null || urlString.trim().isEmpty()) {
            return true;
        }

        // Vérifier la longueur maximale
        if (urlString.length() > MAX_URL_LENGTH) {
            buildViolation(context, "L'URL ne peut pas dépasser " + MAX_URL_LENGTH + " caractères");
            return false;
        }

        try {
            URI uri = URI.create(urlString);
            return validateUriFormat(uri, context);
        } catch (IllegalArgumentException e) {
            buildViolation(context, "L'URL n'est pas bien formée");
            return false;
        }
    }

    /**
     * Valide un URI avec vérification de longueur.
     * Vérifie : null, longueur, format URI.
     *
     * @param uri l'URI à valider
     * @param context le contexte de validation
     * @return true si l'URI est valide, false sinon
     */
    protected boolean validateUriWithLength(URI uri, ConstraintValidatorContext context) {
        // null est considéré comme valide (utiliser @NotNull pour forcer la présence)
        if (uri == null) {
            return true;
        }

        // Vérifier la longueur maximale
        String uriString = uri.toString();
        if (uriString.length() > MAX_URL_LENGTH) {
            buildViolation(context, "L'URL ne peut pas dépasser " + MAX_URL_LENGTH + " caractères");
            return false;
        }

        // Valider le format
        return validateUriFormat(uri, context);
    }

    /**
     * Valide le format d'un URI (protocole et host).
     * Vérifie uniquement : protocole HTTP/HTTPS et présence d'un host.
     *
     * @param uri l'URI à valider
     * @param context le contexte de validation
     * @return true si le format est valide, false sinon
     */
    protected boolean validateUriFormat(URI uri, ConstraintValidatorContext context) {
        if (uri == null) {
            return true;
        }

        String scheme = uri.getScheme();

        // Vérifier que le schéma existe
        if (scheme == null) {
            buildViolation(context, "L'URL doit inclure un protocole (http:// ou https://)");
            return false;
        }

        // Vérifier que le protocole est http ou https
        String schemeLower = scheme.toLowerCase();
        if (!"http".equals(schemeLower) && !"https".equals(schemeLower)) {
            buildViolation(context, "L'URL doit utiliser le protocole HTTP ou HTTPS");
            return false;
        }

        // Vérifier que l'URI a un hôte
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            buildViolation(context, "L'URL doit contenir un nom de domaine valide");
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

