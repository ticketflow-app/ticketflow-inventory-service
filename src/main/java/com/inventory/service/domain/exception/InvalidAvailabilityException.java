package com.inventory.service.domain.exception;

/**
 * Thrown when an availability value is null or negative.
 */
public class InvalidAvailabilityException extends RuntimeException {

    public InvalidAvailabilityException(String message) {
        super(message);
    }
}
