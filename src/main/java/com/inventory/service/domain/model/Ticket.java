package com.inventory.service.domain.model;

import java.time.Instant;

/**
 * Domain model representing an issued ticket.
 */
public class Ticket {

    private final Long id;
    private final Long reservationId;
    private final String userId;
    private final Long eventId;
    private final String sectionId;
    private final String seatNumber;
    private final Instant issuedAt;

    public Ticket(Long id, Long reservationId, String userId, Long eventId,
                  String sectionId, String seatNumber, Instant issuedAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.userId = userId;
        this.eventId = eventId;
        this.sectionId = sectionId;
        this.seatNumber = seatNumber;
        this.issuedAt = issuedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public String getUserId() {
        return userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getSectionId() {
        return sectionId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
