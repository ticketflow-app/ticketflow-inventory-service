package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.port.in.GetAvailabilityUseCase;
import com.inventory.service.domain.exception.NotInitializedException;
import com.inventory.service.infrastructure.adapter.in.controller.dto.AvailabilityResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Reactive REST controller for availability queries.
 * Exposes endpoints under /api/v1/events.
 */
// @RestController (disabled: legacy /api/v1 routes)
public class AvailabilityController {

    private final GetAvailabilityUseCase getAvailabilityUseCase;

    public AvailabilityController(GetAvailabilityUseCase getAvailabilityUseCase) {
        this.getAvailabilityUseCase = getAvailabilityUseCase;
    }

    /**
     * Returns the available capacity for a specific section of an event.
     *
     * @param eventId   the event identifier
     * @param sectionId the section identifier
     * @return HTTP 200 OK with the availability response
     */
    @GetMapping("/api/v1/events/{eventId}/sections/{sectionId}/availability")
    public Mono<AvailabilityResponse> getAvailability(
            @PathVariable String eventId,
            @PathVariable String sectionId) {

        return getAvailabilityUseCase
                .getAvailability(eventId)
                .flatMap(availability -> availability.getLocalities().stream()
                        .filter(locality -> sectionId.equalsIgnoreCase(locality.getLocalityId()))
                        .findFirst()
                        .map(locality -> Mono.just(new AvailabilityResponse(
                                availability.getEventId(),
                                locality.getLocalityId(),
                                locality.getAvailableTickets()
                        )))
                        .orElseGet(() -> Mono.error(
                                new NotInitializedException(
                                        "Availability not initialized for eventId=%s sectionId=%s"
                                                .formatted(eventId, sectionId)))))
                ;
    }
}
