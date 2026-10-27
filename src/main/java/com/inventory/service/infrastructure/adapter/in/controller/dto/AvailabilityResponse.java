package com.inventory.service.infrastructure.adapter.in.controller.dto;

/**
 * Response DTO for availability of a single event section.
 */
public record AvailabilityResponse(
    Long eventId,
    String sectionId,
    Integer availableCapacity
) {}