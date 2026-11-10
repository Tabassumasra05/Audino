package com.audino.exception;

/**
 * Base root checked exception for the Audino Clinical Decision Support & EMR platform.
 */
public class AudinoException extends Exception {

    public AudinoException(String message) {
        super(message);
    }

    public AudinoException(String message, Throwable cause) {
        super(message, cause);
    }
}
