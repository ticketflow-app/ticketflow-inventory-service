package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Reservation;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    /**
     * Sums the quantity of confirmed reservations for an event/locality pair.
     * Used by the reconciliation process (PostgreSQL is the source of truth).
     */
    long sumConfirmedQuantity(Long eventId, String locality);
}
