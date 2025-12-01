package com.wishgifthub.config;

import com.wishgifthub.exception.BusinessRuleException;
import com.wishgifthub.exception.DuplicateResourceException;
import com.wishgifthub.exception.InvalidInvitationException;
import com.wishgifthub.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestionnaire global des exceptions pour l'application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Gère les exceptions ResourceNotFoundException et retourne un code 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Ressource non trouvée : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                "RESOURCE_NOT_FOUND"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gère les exceptions AccessDeniedException personnalisées et retourne un code 403.
     */
    @ExceptionHandler(com.wishgifthub.exception.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCustomAccessDeniedException(com.wishgifthub.exception.AccessDeniedException ex) {
        logger.warn("Accès refusé : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                "ACCESS_DENIED"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gère les exceptions BusinessRuleException et retourne un code 409.
     * Utilisé pour les conflits métier (ex: souhait déjà réservé)
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(BusinessRuleException ex) {
        logger.warn("Règle métier violée : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                "BUSINESS_RULE_VIOLATION"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gère les exceptions InvalidInvitationException et retourne un code 400.
     */
    @ExceptionHandler(InvalidInvitationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInvitationException(InvalidInvitationException ex) {
        logger.warn("Invitation invalide : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                "INVALID_INVITATION"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Gère les exceptions DuplicateResourceException et retourne un code 409.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(DuplicateResourceException ex) {
        logger.warn("Ressource dupliquée : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                "DUPLICATE_RESOURCE"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Gère les exceptions IllegalArgumentException et retourne un code 404.
     * Utilisé notamment quand une ressource (groupe, souhait, etc.) n'est pas trouvée.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Argument illégal : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage() != null ? ex.getMessage() : "Ressource non trouvée",
                "INVALID_ARGUMENT"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Gère les exceptions SecurityException et retourne un code 403.
     * Utilisé notamment quand un utilisateur n'a pas les permissions nécessaires.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        logger.warn("Exception de sécurité : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage() != null ? ex.getMessage() : "Accès refusé",
                "SECURITY_ERROR"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gère les exceptions AccessDeniedException de Spring Security et retourne un code 403.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        logger.warn("Accès Spring Security refusé : {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Accès refusé : permissions insuffisantes",
                "SPRING_ACCESS_DENIED"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Gère toutes les autres exceptions non gérées et retourne un code 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Erreur interne non gérée", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Une erreur interne est survenue",
                "INTERNAL_ERROR"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Classe interne pour structurer les réponses d'erreur.
     */
    public static class ErrorResponse {
        public int status;
        public String message;
        public String code;
        public long timestamp;

        public ErrorResponse(int status, String message, String code) {
            this.status = status;
            this.message = message;
            this.code = code;
            this.timestamp = System.currentTimeMillis();
        }
    }
}

