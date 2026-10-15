package com.inventory.service.domain.exception;

/**
 * Thrown when a reservation is created with a quantity that is null or not greater than zero.
 */
public class InvalidReservationQuantityException extends RuntimeException {

    public InvalidReservationQuantityException(String message) {
        super(message);
    }
}
