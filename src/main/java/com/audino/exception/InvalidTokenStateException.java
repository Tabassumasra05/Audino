package com.audino.exception;

/**
 * Thrown when an illegal state transition is attempted on an outpatient queue token.
 */
public class InvalidTokenStateException extends AudinoException {

    public InvalidTokenStateException(String message) {
        super(message);
    }
}
