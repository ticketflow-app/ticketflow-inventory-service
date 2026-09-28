package com.inventory.service.domain.model;

import java.util.List;

public class Availability {

    private Long eventId;
    private List<LocalityAvailability> localities;

    public Availability(Long eventId, List<LocalityAvailability> localities) {
        this.eventId = eventId;
        this.localities = localities;
    }

    public Long getEventId() {
        return eventId;
    }

    public List<LocalityAvailability> getLocalities() {
        return localities;
    }
}