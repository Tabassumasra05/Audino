package com.audino.exception;

/**
 * Thrown when an authenticated staff account is marked inactive/disabled.
 */
public class AccountDisabledException extends AudinoException {

    public AccountDisabledException(String message) {
        super(message);
    }
}
