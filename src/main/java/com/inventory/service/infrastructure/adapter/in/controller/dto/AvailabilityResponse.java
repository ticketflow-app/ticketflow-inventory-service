package com.inventory.service.infrastructure.adapter.in.controller.dto;

/**
 * Response DTO for availability of a single event locality (legacy /api/v1 contract).
 */
public record AvailabilityResponse(
    String eventId,
    String localityId,
    Integer availableCapacity
) {}
