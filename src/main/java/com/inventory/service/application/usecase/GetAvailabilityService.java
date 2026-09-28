package com.inventory.service.application.usecase;

import com.inventory.service.application.port.in.GetAvailabilityUseCase;
import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.domain.model.Availability;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class GetAvailabilityService implements GetAvailabilityUseCase {
    private final AvailabilityRepository availabilityRepository;

    public GetAvailabilityService(AvailabilityRepository availabilityRepository) {
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    public Mono<Availability> getAvailability(Long eventId) {
        return availabilityRepository.getAvailability(eventId);
    }
}