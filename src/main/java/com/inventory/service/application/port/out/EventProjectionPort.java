package com.inventory.service.application.port.out;

import com.inventory.service.application.port.out.EventLocalitiesPort.EventLocality;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Output port for the local copy of the catalog configuration inventory needs to seed
 * availability counters. The copy is fed by catalog integration events, not by HTTP calls.
 */
public interface EventProjectionPort {

    /** Stores the localities of a published event and schedules its Redis cleanup. */
    Mono<Void> savePublished(String eventId, Instant eventDate, List<EventLocality> localities);

    /** Removes the local projection, its availability counters and its cleanup schedule. */
    Mono<Void> delete(String eventId);

    /** Event ids whose retention deadline has already passed. */
    Flux<String> findExpiredEventIds(Instant now);
}
