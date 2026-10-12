package com.imtidad.exception;

/**
 * Custom exception for database access failures.
 * Wraps SQL and connection errors with user-friendly messages.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
