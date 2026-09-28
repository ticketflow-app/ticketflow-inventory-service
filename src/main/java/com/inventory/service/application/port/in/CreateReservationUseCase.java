package com.inventory.service.application.port.in;

import com.inventory.service.domain.model.Reservation;
import reactor.core.publisher.Mono;

public interface CreateReservationUseCase {

    Mono<Reservation> createReservation(Reservation reservation);
}