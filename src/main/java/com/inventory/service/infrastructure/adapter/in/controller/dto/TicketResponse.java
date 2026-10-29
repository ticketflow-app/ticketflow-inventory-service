package com.inventory.service.infrastructure.adapter.in.controller.dto;

import java.time.Instant;

public record TicketResponse(
    Long id,
    Long reservationId,
    String userId,
    Long eventId,
    String sectionId,
    String seatNumber,
    Instant issuedAt
) {}
