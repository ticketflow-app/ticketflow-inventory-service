package com.inventory.service.infrastructure.adapter.in.controller.dto;

import java.time.Instant;

/**
 * Response DTO for a created reservation.
 */
public record ReservationResponse(
    Long reservationId,
    Long eventId,
    String sectionId,
    Integer quantity,
    String status,
    Instant createdAt
) {}