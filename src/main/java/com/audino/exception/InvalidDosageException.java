package com.audino.exception;

/**
 * Thrown when an entered medication dosage violates the validation grammar
 * or physical limits of the target medication delivery form.
 */
public class InvalidDosageException extends AudinoException {

    public InvalidDosageException(String message) {
        super(message);
    }
}
