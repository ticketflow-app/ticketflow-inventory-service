package com.inventory.service.application.exception;

/**
 * Thrown when the reservation could not be persisted (CU-06 FA-03). The availability
 * has already been compensated (or an incident recorded) before this is raised.
 * Maps to HTTP 503.
 */
public class ReservationPersistenceException extends RuntimeException {

    public ReservationPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
