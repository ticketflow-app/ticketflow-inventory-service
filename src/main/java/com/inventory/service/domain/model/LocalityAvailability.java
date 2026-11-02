package com.inventory.service.domain.model;

import com.inventory.service.domain.exception.InvalidAvailabilityException;

public class LocalityAvailability {

    private final String localityId;
    private final Integer availableTickets;

    public LocalityAvailability(String localityId, Integer availableTickets) {
        if (availableTickets == null || availableTickets < 0) {
            throw new InvalidAvailabilityException(
                    "Available tickets must not be null or negative");
        }
        this.localityId = localityId;
        this.availableTickets = availableTickets;
    }

    public String getLocalityId() {
        return localityId;
    }

    public Integer getAvailableTickets() {
        return availableTickets;
    }
}
