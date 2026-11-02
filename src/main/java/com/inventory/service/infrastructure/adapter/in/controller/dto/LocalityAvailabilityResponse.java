package com.inventory.service.infrastructure.adapter.in.controller.dto;

public record LocalityAvailabilityResponse(
    String localityId,
    Integer availableTickets
) {}
