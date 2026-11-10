package com.audino.exception;

/**
 * Thrown when an attempt is made to approve or persist a prescription containing
 * active, unresolved CRITICAL adverse drug interactions without clinical justification.
 */
public class CriticalInteractionException extends AudinoException {

    public CriticalInteractionException(String message) {
        super(message);
    }
}
