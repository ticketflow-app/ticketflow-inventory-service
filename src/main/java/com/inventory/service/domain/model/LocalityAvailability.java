package com.inventory.service.domain.model;

public class LocalityAvailability {

    private String locality;
    private Integer availableTickets;

    public LocalityAvailability(String locality, Integer availableTickets) {
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