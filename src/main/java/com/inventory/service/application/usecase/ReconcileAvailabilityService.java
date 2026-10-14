package com.inventory.service.application.usecase;

import com.inventory.service.application.port.in.ReconcileAvailabilityUseCase;
import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.LocalityCapacityRepository;
import com.inventory.service.application.port.out.ReservationIncidentRepository;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.LocalityCapacity;
import com.inventory.service.domain.model.ReservationIncident;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Reconciliation of the Redis availability counters against the durable state (DA-001).
 *
 * <p>Expected availability is recomputed as {@code capacity - confirmed reservations},
 * using PostgreSQL (reservations) and the registered capacity as sources of truth. Any
 * drift in Redis (e.g. a phantom reservation whose compensation also failed) is repaired
 * and recorded as an incident.</p>
 */
@Service
public class ReconcileAvailabilityService implements ReconcileAvailabilityUseCase {

    private static final String RECONCILIATION_REPAIR = "RECONCILIATION_REPAIR";

    private final LocalityCapacityRepository localityCapacityRepository;
    private final ReservationRepository reservationRepository;
    private final AvailabilityRepository availabilityRepository;
    private final ReservationIncidentRepository reservationIncidentRepository;

    public ReconcileAvailabilityService(
            LocalityCapacityRepository localityCapacityRepository,
            ReservationRepository reservationRepository,
            AvailabilityRepository availabilityRepository,
            ReservationIncidentRepository reservationIncidentRepository) {
        this.localityCapacityRepository = localityCapacityRepository;
        this.reservationRepository = reservationRepository;
        this.availabilityRepository = availabilityRepository;
        this.reservationIncidentRepository = reservationIncidentRepository;
    }

    @Override
    public void reconcile() {
        for (LocalityCapacity capacity : localityCapacityRepository.findAll()) {
            Long eventId = capacity.getEventId();
            String locality = capacity.getLocality();

            long confirmed = reservationRepository.sumConfirmedQuantity(eventId, locality);
            int expected = (int) Math.max(0, capacity.getCapacity() - confirmed);
            Integer current = availabilityRepository.getCounter(eventId, locality);

            if (current == null || current.intValue() != expected) {
                availabilityRepository.setAvailability(eventId, locality, expected);
                int delta = expected - (current == null ? 0 : current);
                reservationIncidentRepository.save(new ReservationIncident(
                        null, eventId, locality, delta, RECONCILIATION_REPAIR,
                        "Redis availability " + current + " corrected to " + expected
                                + " (capacity " + capacity.getCapacity()
                                + ", confirmed " + confirmed + ")", Instant.now()));
            }
        }
    }
}
