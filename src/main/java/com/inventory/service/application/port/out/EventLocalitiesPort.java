package com.inventory.service.application.port.out;

import reactor.core.publisher.Flux;

/**
 * Output port used to resolve the localities configured for an event, together with their
 * capacity. The static event configuration belongs to the catalog service; inventory only
 * uses it to seed the live counters the first time a locality is queried or reserved.
 */
public interface EventLocalitiesPort {

    Flux<EventLocality> findByEvent(String eventId);

    /**
     * Locality of an event with the capacity declared in the catalog.
     *
     * @param localityId locality identifier (UUID)
     * @param capacity   declared capacity, used as the initial availability
     */
    record EventLocality(String localityId, Integer capacity) {
    }
}
