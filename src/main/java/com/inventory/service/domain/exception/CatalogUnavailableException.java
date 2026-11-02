package com.inventory.service.domain.exception;

/**
 * Raised when the catalog service cannot be reached to resolve the localities of an event.
 */
public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
