package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.port.in.CreateReservationUseCase;
import com.inventory.service.domain.model.Reservation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reservations", description = "Reservation creation for events")
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;

    public ReservationController(CreateReservationUseCase createReservationUseCase) {
        this.createReservationUseCase = createReservationUseCase;
    }

    @Operation(summary = "Create a reservation", description = "Creates a reservation, decrementing availability, and publishes the confirmation event to Kafka.")
    @PostMapping("/reservations")
    public Reservation createReservation(@RequestBody Reservation reservation) {
        return createReservationUseCase.createReservation(reservation);
    }
}
