package com.wishgifthub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand une invitation n'est pas valide.
 * Par exemple : token expiré, invitation déjà acceptée, etc.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidInvitationException extends RuntimeException {

    public InvalidInvitationException(String message) {
        super(message);
    }
}

