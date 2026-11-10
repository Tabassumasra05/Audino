package com.audino.exception;

/**
 * Thrown when user credentials fail cryptographic verification.
 */
public class AuthenticationException extends AudinoException {

    public AuthenticationException(String message) {
        super(message);
    }
}
