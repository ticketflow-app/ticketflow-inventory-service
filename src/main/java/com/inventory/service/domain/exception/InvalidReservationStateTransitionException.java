package com.inventory.service.domain.exception;

/**
 * Thrown when a reservation attempts a state transition that is not allowed by the domain rules.
 */
public class InvalidReservationStateTransitionException extends RuntimeException {

    public InvalidReservationStateTransitionException(String message) {
        super(message);
    }
}
