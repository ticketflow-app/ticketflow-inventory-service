package com.inventory.service.domain.model;

import java.time.Instant;

/**
 * Durable record of an inconsistency incident (CU-06 FA-03, DA-001).
 *
 * <p>Registers persistence failures, compensation outcomes and reconciliation
 * repairs "for analysis", as required by the use case.</p>
 */
public class ReservationIncident {

    private final Long id;
    private final Long eventId;
    private final String locality;
    private final Integer quantity;
    private final String reason;
    private final String detail;
    private final Instant createdAt;

    public ReservationIncident(
            Long id,
            Long eventId,
            String locality,
            Integer quantity,
            String reason,
            String detail,
            Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.locality = locality;
        this.quantity = quantity;
        this.reason = reason;
        this.detail = detail;
        this.createdAt = createdAt;
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

    public Integer getQuantity() {
        return quantity;
    }

    public String getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
