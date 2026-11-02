package com.inventory.service.domain.model;

import java.time.Instant;

/**
 * Domain model representing an issued ticket.
 */
public class Ticket {

    private final Long id;
    private final Long reservationId;
    private final String userId;
    private final String eventId;
    private final String localityId;
    private final String seatNumber;
    private final Instant issuedAt;

    public Ticket(Long id, Long reservationId, String userId, String eventId,
                  String localityId, String seatNumber, Instant issuedAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.userId = userId;
        this.eventId = eventId;
        this.localityId = localityId;
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

    public String getEventId() {
        return eventId;
    }

    public String getLocalityId() {
        return localityId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
