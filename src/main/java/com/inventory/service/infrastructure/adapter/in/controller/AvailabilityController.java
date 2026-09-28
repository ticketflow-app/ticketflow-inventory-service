package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.port.in.GetAvailabilityUseCase;
import com.inventory.service.domain.model.Availability;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AvailabilityController {

    private final GetAvailabilityUseCase getAvailabilityUseCase;

    public AvailabilityController(GetAvailabilityUseCase getAvailabilityUseCase) {
        this.getAvailabilityUseCase = getAvailabilityUseCase;
    }

    @GetMapping("/events/{eventId}/availability")
    public Mono<Availability> getAvailability(@PathVariable Long eventId) {
        return getAvailabilityUseCase.getAvailability(eventId);
    }
}