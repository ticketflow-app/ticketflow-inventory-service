package com.inventory.service.domain.model;

import com.inventory.service.domain.exception.InvalidReservationQuantityException;
import com.inventory.service.domain.exception.InvalidReservationStateTransitionException;

public class Reservation {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final Long id;
    private final String userId;
    private final String eventId;
    private final String localityId;
    private final Integer quantity;
    private final String status;

    public Reservation(
            Long id,
            String userId,
            String eventId,
            String localityId,
            Integer quantity,
            String status) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidReservationQuantityException(
                    "Reservation quantity must be greater than zero");
        }
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.localityId = localityId;
        this.quantity = quantity;
        this.status = status;
    }

    /** Backwards-compatible constructor for flows that do not track the owner. */
    public Reservation(Long id, String eventId, String localityId, Integer quantity, String status) {
        this(id, null, eventId, localityId, quantity, status);
    }

    public Long getId() {
        return id;
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

    public Integer getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }

    /**
     * Confirms the reservation when it is currently pending.
     *
     * @return a new reservation in CONFIRMED state, leaving the original unchanged
     * @throws InvalidReservationStateTransitionException if the reservation is not pending
     */
    public Reservation confirm() {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new InvalidReservationStateTransitionException(
                    "Only a pending reservation can be confirmed");
        }
        return new Reservation(this.id, this.userId, this.eventId, this.localityId,
                this.quantity, STATUS_CONFIRMED);
    }

    /**
     * Cancels the reservation when it is currently pending.
     *
     * @return a new reservation in CANCELLED state, leaving the original unchanged
     * @throws InvalidReservationStateTransitionException if the reservation is not pending
     */
    public Reservation cancel() {
        if (!STATUS_PENDING.equals(this.status)) {
            throw new InvalidReservationStateTransitionException(
                    "Only a pending reservation can be cancelled");
        }
        return new Reservation(this.id, this.userId, this.eventId, this.localityId,
                this.quantity, STATUS_CANCELLED);
    }
}
