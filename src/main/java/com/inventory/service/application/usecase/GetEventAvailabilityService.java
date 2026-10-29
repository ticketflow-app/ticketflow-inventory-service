package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.infrastructure.adapter.in.controller.dto.EventAvailabilityResponse;
import com.inventory.service.infrastructure.adapter.in.controller.dto.SectionAvailability;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GetEventAvailabilityService {

    private final AvailabilityRepository availabilityRepository;

    public GetEventAvailabilityService(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    public Mono<EventAvailabilityResponse> getAvailability(Long eventId) {
        return availabilityRepository.getAvailability(eventId)
                .map(availability -> new EventAvailabilityResponse(
                        availability.getEventId(),
                        availability.getLocalities().stream()
                                .map(locality -> new SectionAvailability(
                                        locality.getLocality(),
                                        locality.getAvailableTickets()))
                                .toList()))
                .switchIfEmpty(Mono.error(new com.inventory.service.domain.exception.ResourceNotFoundException(
                        "Availability not found for eventId=" + eventId)));
    }
}
