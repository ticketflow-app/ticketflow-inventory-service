package com.inventory.service.domain.model;

import com.inventory.service.domain.exception.InvalidAvailabilityException;

public class LocalityAvailability {

    private final String locality;
    private final Integer availableTickets;

    public LocalityAvailability(String locality, Integer availableTickets) {
        if (availableTickets == null || availableTickets < 0) {
            throw new InvalidAvailabilityException(
                    "Available tickets must not be null or negative");
        }
        this.locality = locality;
        this.availableTickets = availableTickets;
    }

    public String getLocality() {
        return locality;
    }

    public Integer getAvailableTickets() {
        return availableTickets;
    }
}