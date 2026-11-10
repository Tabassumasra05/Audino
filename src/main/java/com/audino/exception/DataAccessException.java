package com.audino.exception;

/**
 * Runtime exception wrapping low-level JDBC SQLExceptions, decoupling the
 * service and presentation tiers from relational database persistence details.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
