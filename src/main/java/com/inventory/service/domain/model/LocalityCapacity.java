package com.inventory.service.domain.model;

/**
 * Static capacity configured for a locality of an event (HU-11).
 *
 * <p>The capacity is the durable reference used by the reconciliation process to
 * recompute the expected availability in Redis (PostgreSQL is the source of truth
 * for reservations, the capacity is the source of truth for the maximum).</p>
 */
public class LocalityCapacity {

    private final Long id;
    private final Long eventId;
    private final String locality;
    private final Integer capacity;

    public LocalityCapacity(Long id, Long eventId, String locality, Integer capacity) {
        this.id = id;
        this.eventId = eventId;
        this.locality = locality;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getLocality() {
        return locality;
    }

    public Integer getCapacity() {
        return capacity;
    }
}
