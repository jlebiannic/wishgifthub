package com.wishgifthub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand un utilisateur tente d'accéder à une ressource
 * pour laquelle il n'a pas les permissions nécessaires.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("Accès refusé : vous n'avez pas les permissions nécessaires");
    }
}

