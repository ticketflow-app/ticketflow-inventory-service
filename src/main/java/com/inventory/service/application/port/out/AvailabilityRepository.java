package com.inventory.service.application.port.out;

import reactor.core.publisher.Mono;

/**
 * Output port for the live availability counters. The counters live in Redis and are
 * keyed by event and locality, because catalog identifies both aggregates with UUIDs.
 */
public interface AvailabilityRepository {

    /**
     * Reads the current availability of an event locality.
     *
     * @param eventId    the event identifier (required)
     * @param localityId the locality identifier (required)
     * @return the available units, or {@code 0} when the counter was never initialized
     */
    Mono<Integer> readAvailability(String eventId, String localityId);

    /**
     * Seeds the availability of an event locality only if it does not exist yet.
     *
     * @param eventId    the event identifier (required)
     * @param localityId the locality identifier (required, non-blank)
     * @param capacity   the initial capacity to seed (required, greater than or equal to zero)
     * @return {@link InitializeOutcome#INITIALIZED} if created, {@link InitializeOutcome#ALREADY_INITIALIZED} otherwise
     */
    Mono<InitializeOutcome> initialize(String eventId, String localityId, Integer capacity);

    /**
     * Atomically reserves {@code quantity} units of an event locality if enough availability exists.
     *
     * @param eventId    the event identifier (required)
     * @param localityId the locality identifier (required)
     * @param quantity   the units to reserve (required, greater than zero)
     * @return a {@link ReservationOutcome} describing the result
     */
    Mono<ReservationOutcome> tryReserve(String eventId, String localityId, Integer quantity);

    /**
     * Atomically releases {@code quantity} units back to an event locality, capped at its initial capacity.
     *
     * @param eventId    the event identifier (required)
     * @param localityId the locality identifier (required)
     * @param quantity   the units to release (required, greater than zero)
     * @return a {@link ReleaseOutcome} describing the result
     */
    Mono<ReleaseOutcome> release(String eventId, String localityId, Integer quantity);

    /**
     * Overrides the availability and capacity of an event locality (administrative adjustment).
     *
     * @param eventId     the event identifier (required)
     * @param localityId  the locality identifier (required)
     * @param newQuantity the new available quantity (required, greater than or equal to zero)
     */
    Mono<Void> adjust(String eventId, String localityId, Integer newQuantity);
}
