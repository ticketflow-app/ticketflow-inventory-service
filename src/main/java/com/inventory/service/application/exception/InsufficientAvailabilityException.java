package com.inventory.service.application.exception;

/**
 * Thrown when the requested quantity cannot be reserved because there is not
 * enough availability (CU-06 FA-01). Maps to HTTP 409.
 */
public class InsufficientAvailabilityException extends RuntimeException {

    public InsufficientAvailabilityException(Long eventId, String locality, Integer quantity) {
        super("Insufficient availability for event " + eventId + ", locality '" + locality
                + "' (requested " + quantity + ")");
    }
}
