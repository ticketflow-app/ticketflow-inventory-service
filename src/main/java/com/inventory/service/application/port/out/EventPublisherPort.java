package com.inventory.service.application.port.out;

import com.inventory.service.application.domain.event.ReservationConfirmedEvent;
import reactor.core.publisher.Mono;

/**
 * Output port for publishing reservation confirmation events to the event stream (Kafka).
 * Implementations must be non-blocking and resilient to broker failures.
 */
public interface EventPublisherPort {

    /**
     * Publishes a {@link ReservationConfirmedEvent} to the event stream.
     *
     * @param event the reservation confirmed event to publish
     * @return {@link Mono} that completes when the event has been handed to the producer.
     *         Implementations must NOT propagate broker failures to the caller;
     *         they must log and swallow errors so the reservation flow is not broken.
     */
    Mono<Void> publishReservationConfirmed(ReservationConfirmedEvent event);
}