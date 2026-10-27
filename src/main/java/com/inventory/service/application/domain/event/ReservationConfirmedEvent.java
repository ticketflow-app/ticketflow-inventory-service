package com.inventory.service.application.domain.event;

import java.time.Instant;

/**
 * Event published when a reservation is successfully confirmed and persisted.
 * Carries all information needed by downstream consumers (e.g., notification, billing).
 */
public record ReservationConfirmedEvent(
    String reservationId,
    Long eventId,
    String sectionId,
    Integer quantity,
    Instant timestamp,
    String correlationId
) {}