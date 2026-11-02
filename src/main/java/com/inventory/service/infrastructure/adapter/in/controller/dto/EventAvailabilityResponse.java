package com.inventory.service.infrastructure.adapter.in.controller.dto;

import java.util.List;

public record EventAvailabilityResponse(
    String eventId,
    List<LocalityAvailabilityResponse> localities
) {}
