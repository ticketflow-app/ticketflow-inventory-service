package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Availability;
import reactor.core.publisher.Mono;

public interface AvailabilityRepository {

    Mono<Availability> getAvailability(Long eventId);
}