package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Reservation;
import reactor.core.publisher.Mono;

public interface ReservationEventPublisher {

    Mono<Void> publish(Reservation reservation);
}