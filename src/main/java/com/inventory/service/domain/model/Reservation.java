package com.inventory.service.domain.model;

import com.inventory.service.domain.exception.InvalidReservationQuantityException;
import com.inventory.service.domain.exception.InvalidReservationStateTransitionException;

public class Reservation {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final Long id;
    private final Long eventId;
    private final String locality;
    private final Integer quantity;
    private final String status;

    public Reservation(
            Long id,
            Long eventId,
            String locality,
            Integer quantity,
            String status) {
        if (quantity == null || quantity <= 0) {
            throw new InvalidReservationQuantityException(
                    "Reservation quantity must be greater than zero");
        }
        this.id = id;
        this.eventId = eventId;
        this.locality = locality;
        this.quantity = quantity;
        this.status = status;
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
        return new Reservation(this.id, this.eventId, this.locality, this.quantity, STATUS_CONFIRMED);
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
        return new Reservation(this.id, this.eventId, this.locality, this.quantity, STATUS_CANCELLED);
    }
}