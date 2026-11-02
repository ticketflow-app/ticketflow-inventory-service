package com.inventory.service.domain.model;

import java.util.List;

public class Availability {

    private final String eventId;
    private final List<LocalityAvailability> localities;

    public Availability(String eventId, List<LocalityAvailability> localities) {
        this.eventId = eventId;
        this.localities = localities;
    }

    public String getEventId() {
        return eventId;
    }

    public List<LocalityAvailability> getLocalities() {
        return localities;
    }
}
