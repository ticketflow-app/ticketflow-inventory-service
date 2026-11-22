package com.inventory.service.application.port.out;

import reactor.core.publisher.Flux;

/**
 * Output port used to resolve the localities configured for an event, together with their
 * capacity. The static event configuration belongs to the catalog service; inventory keeps a
 * local projection fed by Kafka integration events and uses it only to seed live counters.
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
