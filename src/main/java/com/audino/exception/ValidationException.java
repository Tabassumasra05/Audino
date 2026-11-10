package com.audino.exception;

/**
 * Thrown when domain entities or clinical inputs fail business validation criteria.
 */
public class ValidationException extends AudinoException {

    public ValidationException(String message) {
        super(message);
    }
}
