package com.inventory.service.domain.exception;

public class EventNotSellableException extends RuntimeException {
    public EventNotSellableException(String message) {
        super(message);
    }
}
