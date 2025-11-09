package com.wishgifthub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand une règle métier est violée.
 * Par exemple : réserver son propre souhait, réserver un souhait déjà réservé, etc.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

