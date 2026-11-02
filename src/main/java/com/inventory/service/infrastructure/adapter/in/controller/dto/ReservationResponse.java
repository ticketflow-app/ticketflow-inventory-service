package com.inventory.service.infrastructure.adapter.in.controller.dto;

import java.time.Instant;

public record ReservationResponse(
    Long id,
    String userId,
    String eventId,
    String localityId,
    Integer quantity,
    String status,
    Long remainingSeconds,
    Instant expiresAt
) {}
