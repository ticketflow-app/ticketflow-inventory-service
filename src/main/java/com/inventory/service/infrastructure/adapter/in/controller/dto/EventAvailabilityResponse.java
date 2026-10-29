package com.inventory.service.infrastructure.adapter.in.controller.dto;

import java.util.List;

public record EventAvailabilityResponse(
    Long eventId,
    List<SectionAvailability> sections
) {}
