package com.inventory.service.application.usecase;

import com.inventory.service.application.exception.InsufficientAvailabilityException;
import com.inventory.service.application.exception.ReservationPersistenceException;
import com.inventory.service.application.port.in.CreateReservationUseCase;
import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationIncidentRepository;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.Reservation;
import com.inventory.service.domain.model.ReservationIncident;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Creates a reservation using the critical atomic flow defined by the architecture
 * (§10.4) plus the recovery mechanism of DA-001.
 *
 * <ol>
 *   <li>Atomically decrement availability in Redis ({@code DECRBY}). If the counter goes
 *       below zero, restore it ({@code INCRBY}) and reject the request (CU-06 FA-01).</li>
 *   <li>Persist the reservation in PostgreSQL (audit log).</li>
 *   <li>If persistence fails, compensate Redis ({@code INCRBY}) and record the incident
 *       (CU-06 FA-03, DA-001).</li>
 *   <li>Publish the confirmed reservation event to Kafka (best effort in phase 1).</li>
 * </ol>
 */
@Service
public class CreateReservationService implements CreateReservationUseCase {

    private static final String PERSISTENCE_FAILURE_COMPENSATED = "PERSISTENCE_FAILURE_COMPENSATED";
    private static final String COMPENSATION_FAILED = "COMPENSATION_FAILED";
    private static final String EVENT_PUBLISH_FAILURE = "EVENT_PUBLISH_FAILURE";

    private final AvailabilityRepository availabilityRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher reservationEventPublisher;
    private final ReservationIncidentRepository reservationIncidentRepository;

    public CreateReservationService(
            AvailabilityRepository availabilityRepository,
            ReservationRepository reservationRepository,
            ReservationEventPublisher reservationEventPublisher,
            ReservationIncidentRepository reservationIncidentRepository) {
        this.availabilityRepository = availabilityRepository;
        this.reservationRepository = reservationRepository;
        this.reservationEventPublisher = reservationEventPublisher;
        this.reservationIncidentRepository = reservationIncidentRepository;
    }

    @Override
    public Reservation createReservation(Reservation reservation) {
        Long eventId = reservation.getEventId();
        String locality = reservation.getLocality();
        Integer quantity = reservation.getQuantity();

        if (eventId == null || locality == null || locality.isBlank()
                || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "eventId, locality and a positive quantity are required");
        }

        boolean reserved = availabilityRepository.tryReserve(eventId, locality, quantity);
        if (!reserved) {
            throw new InsufficientAvailabilityException(eventId, locality, quantity);
        }

        Reservation saved;
        try {
            saved = reservationRepository.save(reservation);
        } catch (RuntimeException persistenceFailure) {
            compensate(eventId, locality, quantity, persistenceFailure);
            throw new ReservationPersistenceException(
                    "Failed to persist reservation for event " + eventId
                            + ", locality '" + locality + "'", persistenceFailure);
        }

        publishQuietly(saved);

        return saved;
    }

    /**
     * Restores the availability consumed by a reservation whose persistence failed.
     * A second incident is recorded if the compensation itself fails, so the drift can
     * be detected and repaired by the reconciliation process.
     */
    private void compensate(Long eventId, String locality, Integer quantity, RuntimeException cause) {
        try {
            availabilityRepository.release(eventId, locality, quantity);
            reservationIncidentRepository.save(new ReservationIncident(
                    null, eventId, locality, quantity, PERSISTENCE_FAILURE_COMPENSATED,
                    cause.getClass().getSimpleName() + ": " + cause.getMessage(), Instant.now()));
        } catch (RuntimeException compensationFailure) {
            reservationIncidentRepository.save(new ReservationIncident(
                    null, eventId, locality, quantity, COMPENSATION_FAILED,
                    "Persistence: " + cause.getMessage()
                            + " | Compensation: " + compensationFailure.getMessage(), Instant.now()));
        }
    }

    /**
     * Publishing is best effort in this phase: the reservation is already durable and the
     * availability is consistent, so a broker outage must not roll back the operation.
     * The failure is recorded for analysis instead.
     */
    private void publishQuietly(Reservation reservation) {
        try {
            reservationEventPublisher.publish(reservation);
        } catch (RuntimeException publishFailure) {
            reservationIncidentRepository.save(new ReservationIncident(
                    null, reservation.getEventId(), reservation.getLocality(), reservation.getQuantity(),
                    EVENT_PUBLISH_FAILURE,
                    publishFailure.getClass().getSimpleName() + ": " + publishFailure.getMessage(),
                    Instant.now()));
        }
    }
}
