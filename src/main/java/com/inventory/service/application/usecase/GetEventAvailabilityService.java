package com.inventory.service.application.usecase;

import com.inventory.service.application.port.in.GetAvailabilityUseCase;
import com.inventory.service.infrastructure.adapter.in.controller.dto.EventAvailabilityResponse;
import com.inventory.service.infrastructure.adapter.in.controller.dto.LocalityAvailabilityResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class GetEventAvailabilityService {

    private final GetAvailabilityUseCase getAvailabilityUseCase;

    public GetEventAvailabilityService(GetAvailabilityUseCase getAvailabilityUseCase) {
        this.getAvailabilityUseCase = getAvailabilityUseCase;
    }

    public Mono<EventAvailabilityResponse> getAvailability(String eventId) {
        return getAvailabilityUseCase.getAvailability(eventId)
                .map(availability -> new EventAvailabilityResponse(
                        availability.getEventId(),
                        availability.getLocalities().stream()
                                .map(locality -> new LocalityAvailabilityResponse(
                                        locality.getLocalityId(),
                                        locality.getAvailableTickets()))
                                .toList()));
    }
}
