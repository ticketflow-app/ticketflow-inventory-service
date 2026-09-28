package com.inventory.service.application.usecase;

import com.inventory.service.application.port.in.CreateReservationUseCase;
import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.Reservation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CreateReservationService implements CreateReservationUseCase {

    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher reservationEventPublisher;

    public CreateReservationService(
            ReservationRepository reservationRepository,
            ReservationEventPublisher reservationEventPublisher) {
        this.reservationRepository = reservationRepository;
        this.reservationEventPublisher = reservationEventPublisher;
    }

    @Override
    public Mono<Reservation> createReservation(Reservation reservation) {
        return reservationRepository.save(reservation)
                .flatMap(savedReservation ->
                        reservationEventPublisher.publish(savedReservation)
                                .thenReturn(savedReservation)
                );
    }
}