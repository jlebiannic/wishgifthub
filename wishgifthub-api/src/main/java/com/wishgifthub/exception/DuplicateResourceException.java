package com.wishgifthub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand un utilisateur tente de créer une ressource qui existe déjà.
 * Par exemple : créer un utilisateur avec un email déjà utilisé.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceType, String field, String value) {
        super(String.format("%s avec %s '%s' existe déjà", resourceType, field, value));
    }
}

