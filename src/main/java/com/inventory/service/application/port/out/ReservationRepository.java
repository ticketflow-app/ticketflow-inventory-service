package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Reservation;
import reactor.core.publisher.Mono;

public interface ReservationRepository {

    Mono<Reservation> save(Reservation reservation);
}