package com.inventory.service.application.usecase;

import com.inventory.service.application.port.in.CreateReservationUseCase;
import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationOutcome;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.exception.InvalidAvailabilityException;
import com.inventory.service.domain.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Creates reservations following the short-saga approved in DA-001: atomic availability
 * reservation in Redis first, then persistence in PostgreSQL, compensating the Redis stock
 * when persistence fails, and publishing the event only after a successful persistence.
 */
@Service
public class CreateReservationService implements CreateReservationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateReservationService.class);

    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher reservationEventPublisher;
    private final AvailabilityRepository availabilityRepository;

    public CreateReservationService(
            ReservationRepository reservationRepository,
            ReservationEventPublisher reservationEventPublisher,
            AvailabilityRepository availabilityRepository) {
        this.reservationRepository = reservationRepository;
        this.reservationEventPublisher = reservationEventPublisher;
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    public Mono<Reservation> createReservation(Reservation reservation) {
        return availabilityRepository
                .tryReserve(reservation.getEventId(), reservation.getLocality(), reservation.getQuantity())
                .flatMap(outcome -> {
                    if (outcome != ReservationOutcome.RESERVED) {
                        return Mono.error(rejection(outcome, reservation));
                    }
                    return reservationRepository.save(reservation)
                            .onErrorResume(persistenceFailure ->
                                    compensate(reservation, persistenceFailure)
                                            .then(Mono.error(persistenceFailure)))
                            .flatMap(savedReservation ->
                                    reservationEventPublisher.publish(savedReservation)
                                            .thenReturn(savedReservation));
                });
    }

    /**
     * Restores the Redis stock when PostgreSQL persistence fails (DA-001 compensating action).
     * A compensation failure is logged as an error incident (CU-06 FA-03) and swallowed so the
     * original persistence failure is the one propagated to the caller; the stock left
     * sub-estimated is detected and corrected by the future reconciliation job.
     */
    private Mono<Void> compensate(Reservation reservation, Throwable persistenceFailure) {
        log.error("Reservation persistence failed (eventId={}, locality={}, quantity={}); "
                        + "compensating Redis availability. Cause: {}",
                reservation.getEventId(), reservation.getLocality(), reservation.getQuantity(),
                persistenceFailure.toString());
        return availabilityRepository
                .release(reservation.getEventId(), reservation.getLocality(), reservation.getQuantity())
                .doOnNext(outcome -> log.info(
                        "Compensation completed with outcome {} for eventId={}, locality={}, quantity={}",
                        outcome, reservation.getEventId(), reservation.getLocality(), reservation.getQuantity()))
                .doOnError(compensationFailure -> log.error(
                        "Compensation FAILED for eventId={}, locality={}, quantity={}. "
                                + "Manual reconciliation required. Cause: {}",
                        reservation.getEventId(), reservation.getLocality(), reservation.getQuantity(),
                        compensationFailure.toString()))
                .onErrorResume(compensationFailure -> Mono.empty())
                .then();
    }

    private RuntimeException rejection(ReservationOutcome outcome, Reservation reservation) {
        String message = switch (outcome) {
            case INSUFFICIENT_AVAILABILITY -> "Insufficient availability for eventId=%s locality=%s quantity=%s"
                    .formatted(reservation.getEventId(), reservation.getLocality(), reservation.getQuantity());
            case NOT_INITIALIZED -> "Availability not initialized for eventId=%s locality=%s"
                    .formatted(reservation.getEventId(), reservation.getLocality());
            default -> "Reservation rejected with outcome %s".formatted(outcome);
        };
        return new InvalidAvailabilityException(message);
    }
}