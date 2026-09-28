package com.inventory.service.application.port.in;

import com.inventory.service.domain.model.Availability;

import reactor.core.publisher.Mono;

public interface GetAvailabilityUseCase {
    Mono<Availability> getAvailability(Long eventId);
}
