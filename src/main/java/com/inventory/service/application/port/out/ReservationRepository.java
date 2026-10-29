package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Reservation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReservationRepository {

    Mono<Reservation> save(Reservation reservation);

    default Mono<Reservation> findById(Long id) {
        return Mono.empty();
    }

    default Mono<Reservation> findByIdempotencyKey(String idempotencyKey) {
        return Mono.empty();
    }

    default Flux<Reservation> findByUserId(String userId) {
        return Flux.empty();
    }
}