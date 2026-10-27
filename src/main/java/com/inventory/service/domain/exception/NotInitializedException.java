package com.inventory.service.domain.exception;

/**
 * Thrown when the availability of an event section has not been initialized yet.
 * Maps to HTTP 400 Bad Request in the REST layer.
 */
public class NotInitializedException extends RuntimeException {

    public NotInitializedException(String message) {
        super(message);
    }
}