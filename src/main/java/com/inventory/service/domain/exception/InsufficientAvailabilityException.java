package com.inventory.service.domain.exception;

/**
 * Thrown when there is not enough availability to fulfill a reservation request.
 * Maps to HTTP 409 Conflict in the REST layer.
 */
public class InsufficientAvailabilityException extends RuntimeException {

    public InsufficientAvailabilityException(String message) {
        super(message);
    }
}