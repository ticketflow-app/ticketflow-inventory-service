package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.port.in.CreateReservationUseCase;
import com.inventory.service.domain.model.Reservation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;

    public ReservationController(CreateReservationUseCase createReservationUseCase) {
        this.createReservationUseCase = createReservationUseCase;
    }

    @PostMapping("/reservations")
    public Mono<Reservation> createReservation(@RequestBody Reservation reservation) {
        return createReservationUseCase.createReservation(reservation);
    }
}