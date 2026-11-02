package com.inventory.service.application.usecase;

import com.inventory.service.application.domain.event.ReservationConfirmedEvent;
import com.inventory.service.application.port.in.CreateReservationUseCase;
import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.EventPublisherPort;
import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationOutcome;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.exception.InsufficientAvailabilityException;
import com.inventory.service.domain.exception.InvalidAvailabilityException;
import com.inventory.service.domain.exception.NotInitializedException;
import com.inventory.service.domain.model.Reservation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Creates reservations following the short-saga approved in DA-001: atomic availability
 * reservation in Redis first, then persistence in PostgreSQL, compensating the Redis stock
 * when persistence fails, and publishing the ReservationConfirmedEvent to Kafka after a
 * successful persistence. Kafka failures are logged and swallowed — they never break the
 * client-facing reservation flow.
 */
@Service
public class CreateReservationService implements CreateReservationUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateReservationService.class);

    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher reservationEventPublisher;
    private final EventPublisherPort eventPublisherPort;
    private final AvailabilityRepository availabilityRepository;
    private final Counter reservationsCreatedCounter;
    private final Counter reservationsRejectedCounter;
    private final Timer reservationsLatencyTimer;
    private final MeterRegistry meterRegistry;

    public CreateReservationService(
            ReservationRepository reservationRepository,
            ReservationEventPublisher reservationEventPublisher,
            EventPublisherPort eventPublisherPort,
            AvailabilityRepository availabilityRepository,
            MeterRegistry meterRegistry) {
        this.reservationRepository = reservationRepository;
        this.reservationEventPublisher = reservationEventPublisher;
        this.eventPublisherPort = eventPublisherPort;
        this.availabilityRepository = availabilityRepository;
        this.meterRegistry = meterRegistry;
        this.reservationsCreatedCounter = Counter.builder("reservations.created.total")
                .tag("application", "ticketflow-inventory-service")
                .register(meterRegistry);
        this.reservationsRejectedCounter = Counter.builder("reservations.rejected.total")
                .tag("application", "ticketflow-inventory-service")
                .register(meterRegistry);
        this.reservationsLatencyTimer = Timer.builder("reservations.latency")
                .tag("application", "ticketflow-inventory-service")
                .register(meterRegistry);
    }

    @Override
    public Mono<Reservation> createReservation(Reservation reservation) {
        Timer.Sample sample = Timer.start();
        return availabilityRepository
                .tryReserve(reservation.getEventId(), reservation.getLocalityId(), reservation.getQuantity())
                .timeout(Duration.ofSeconds(3))
                .flatMap(outcome -> {
                    if (outcome != ReservationOutcome.RESERVED) {
                        String reason = switch (outcome) {
                            case INSUFFICIENT_AVAILABILITY -> "insufficient_availability";
                            case NOT_INITIALIZED -> "not_initialized";
                            default -> "unknown";
                        };
                        Counter.builder("reservations.rejected").tag("reason", reason).register(meterRegistry).increment();
                        return Mono.error(rejection(outcome, reservation));
                    }
                    return reservationRepository.save(reservation)
                            .timeout(Duration.ofSeconds(5))
                            .onErrorResume(persistenceFailure ->
                                    compensate(reservation, persistenceFailure)
                                            .then(Mono.error(persistenceFailure)))
                            .flatMap(savedReservation -> publishAndReturn(savedReservation));
                })
                .doOnSuccess(saved -> {
                    Counter.builder("reservations.created.total")
                            .tag("eventId", saved.getEventId().toString())
                            .tag("localityId", saved.getLocalityId())
                            .tag("status", saved.getStatus())
                            .register(meterRegistry)
                            .increment();
                    sample.stop(reservationsLatencyTimer);
                })
                .doOnError(err -> sample.stop(reservationsLatencyTimer));
    }

    /**
     * Publishes the {@link ReservationConfirmedEvent} to Kafka and returns the saved reservation.
     * Kafka failures are logged and swallowed — they never break the client-facing flow.
     */
    private Mono<Reservation> publishAndReturn(Reservation savedReservation) {
        ReservationConfirmedEvent event = new ReservationConfirmedEvent(
                savedReservation.getId() != null ? savedReservation.getId().toString() : null,
                savedReservation.getEventId(),
                savedReservation.getLocalityId(),
                savedReservation.getQuantity(),
                Instant.now(),
                "corr-" + (savedReservation.getId() != null ? savedReservation.getId() : "unknown")
        );

        return eventPublisherPort
                .publishReservationConfirmed(event)
                .onErrorResume(kafkaFailure -> {
                    log.error(
                            "Kafka event publication failed (eventId={}, reservationId={}, quantity={}); " +
                                    "reservation remains confirmed. The incident is logged for reconciliation. Cause: {}",
                            savedReservation.getEventId(),
                            savedReservation.getId(),
                            savedReservation.getQuantity(),
                            kafkaFailure.getMessage(),
                            kafkaFailure);
                    return Mono.empty();
                })
                .thenReturn(savedReservation);
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
                reservation.getEventId(), reservation.getLocalityId(), reservation.getQuantity(),
                persistenceFailure.toString());
        return availabilityRepository
                .release(reservation.getEventId(), reservation.getLocalityId(), reservation.getQuantity())
                .timeout(Duration.ofSeconds(3))
                .doOnNext(outcome -> log.info(
                        "Compensation completed with outcome {} for eventId={}, locality={}, quantity={}",
                        outcome, reservation.getEventId(), reservation.getLocalityId(), reservation.getQuantity()))
                .doOnError(compensationFailure -> log.error(
                        "Compensation FAILED for eventId={}, locality={}, quantity={}. "
                                + "Manual reconciliation required. Cause: {}",
                        reservation.getEventId(), reservation.getLocalityId(), reservation.getQuantity(),
                        compensationFailure.toString()))
                .onErrorResume(compensationFailure -> Mono.empty())
                .then();
    }

    private RuntimeException rejection(ReservationOutcome outcome, Reservation reservation) {
        String message = switch (outcome) {
            case INSUFFICIENT_AVAILABILITY -> "Insufficient availability for eventId=%s locality=%s quantity=%s"
                    .formatted(reservation.getEventId(), reservation.getLocalityId(), reservation.getQuantity());
            case NOT_INITIALIZED -> "Availability not initialized for eventId=%s locality=%s"
                    .formatted(reservation.getEventId(), reservation.getLocalityId());
            default -> "Reservation rejected with outcome %s".formatted(outcome);
        };
        return switch (outcome) {
            case INSUFFICIENT_AVAILABILITY -> new InsufficientAvailabilityException(message);
            case NOT_INITIALIZED -> new NotInitializedException(message);
            default -> new InvalidAvailabilityException(message);
        };
    }
}
