package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Availability;
import reactor.core.publisher.Mono;

public interface AvailabilityRepository {

    Mono<Availability> getAvailability(Long eventId);

    /**
     * Seeds the availability of an event section only if it does not exist yet.
     *
     * @param eventId   the event identifier (required)
     * @param sectionId the section identifier (required, non-blank)
     * @param capacity  the initial capacity to seed (required, greater than or equal to zero)
     * @return {@link InitializeOutcome#INITIALIZED} if created, {@link InitializeOutcome#ALREADY_INITIALIZED} otherwise
     */
    Mono<InitializeOutcome> initialize(Long eventId, String sectionId, Integer capacity);

    /**
     * Atomically reserves {@code quantity} units of an event section if enough availability exists.
     *
     * @param eventId   the event identifier (required)
     * @param sectionId the section identifier (required, non-blank)
     * @param quantity  the units to reserve (required, greater than zero)
     * @return a {@link ReservationOutcome} describing the result
     */
    Mono<ReservationOutcome> tryReserve(Long eventId, String sectionId, Integer quantity);

    /**
     * Atomically releases {@code quantity} units back to an event section, capped at its initial capacity.
     *
     * @param eventId   the event identifier (required)
     * @param sectionId the section identifier (required, non-blank)
     * @param quantity  the units to release (required, greater than zero)
     * @return a {@link ReleaseOutcome} describing the result
     */
    Mono<ReleaseOutcome> release(Long eventId, String sectionId, Integer quantity);
}
