package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.port.in.GetAvailabilityUseCase;
import com.inventory.service.domain.model.Availability;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Availability", description = "Real-time availability queries per event")
public class AvailabilityController {

    private final GetAvailabilityUseCase getAvailabilityUseCase;

    public AvailabilityController(GetAvailabilityUseCase getAvailabilityUseCase) {
        this.getAvailabilityUseCase = getAvailabilityUseCase;
    }

    @Operation(summary = "Get availability for an event", description = "Returns the available tickets per locality for the given event.")
    @GetMapping("/events/{eventId}/availability")
    public Availability getAvailability(@PathVariable Long eventId) {
        return getAvailabilityUseCase.getAvailability(eventId);
    }
}
