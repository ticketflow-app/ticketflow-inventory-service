package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.port.in.CreateReservationUseCase;
import com.inventory.service.domain.model.Reservation;
import com.inventory.service.infrastructure.adapter.in.controller.dto.ReservationRequest;
import com.inventory.service.infrastructure.adapter.in.controller.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

/**
 * Reactive REST controller for reservation operations.
 * Exposes endpoints under /api/v1/events.
 */
// @RestController (disabled: legacy /api/v1 routes)
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;

    public ReservationController(CreateReservationUseCase createReservationUseCase) {
        this.createReservationUseCase = createReservationUseCase;
    }

    /**
     * Creates a reservation for a given event and section.
     *
     * @param eventId  the event identifier
     * @param sectionId the section identifier
     * @param request  the reservation request (validated)
     * @return HTTP 201 Created with the reservation response
     */
    @PostMapping("/api/v1/events/{eventId}/sections/{sectionId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReservationResponse> createReservation(
            @PathVariable String eventId,
            @PathVariable String sectionId,
            @Valid @RequestBody ReservationRequest request) {

        Reservation domainReservation = new Reservation(
                null, eventId, sectionId, request.quantity(), Reservation.STATUS_PENDING);

        return createReservationUseCase
                .createReservation(domainReservation)
                .map(saved -> new ReservationResponse(
        saved.getId(),
        null,
        saved.getEventId(),
        saved.getLocalityId(),
        saved.getQuantity(),
        saved.getStatus(),
        null,
        null
));
    }
}
