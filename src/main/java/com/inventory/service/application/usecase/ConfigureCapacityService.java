package com.inventory.service.application.usecase;

import com.inventory.service.application.port.in.ConfigureCapacityUseCase;
import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.LocalityCapacityRepository;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.LocalityCapacity;
import org.springframework.stereotype.Service;

/**
 * Registers the capacity of a locality (HU-11) and initializes the Redis availability
 * counter to {@code capacity - confirmed reservations}. This is the mechanism used to
 * seed availability for the concurrency tests and by the (future) Catalog service when
 * it initializes the counters of a new event.
 */
@Service
public class ConfigureCapacityService implements ConfigureCapacityUseCase {

    private final LocalityCapacityRepository localityCapacityRepository;
    private final ReservationRepository reservationRepository;
    private final AvailabilityRepository availabilityRepository;

    public ConfigureCapacityService(
            LocalityCapacityRepository localityCapacityRepository,
            ReservationRepository reservationRepository,
            AvailabilityRepository availabilityRepository) {
        this.localityCapacityRepository = localityCapacityRepository;
        this.reservationRepository = reservationRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    public void configure(Long eventId, String locality, int capacity) {
        if (eventId == null || locality == null || locality.isBlank()) {
            throw new IllegalArgumentException("eventId and locality are required");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be greater than or equal to zero");
        }

        LocalityCapacity saved = localityCapacityRepository.save(
                new LocalityCapacity(null, eventId, locality, capacity));

        long confirmed = reservationRepository.sumConfirmedQuantity(eventId, locality);
        int available = (int) Math.max(0, saved.getCapacity() - confirmed);

        availabilityRepository.setAvailability(eventId, locality, available);
    }
}
