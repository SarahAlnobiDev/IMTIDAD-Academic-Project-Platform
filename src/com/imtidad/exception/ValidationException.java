package com.imtidad.exception;

/**
 * Custom exception for domain/business validation failures.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
