package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.EventPublisherPort;
import com.inventory.service.application.port.out.InitializeOutcome;
import com.inventory.service.application.port.out.ReleaseOutcome;
import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationOutcome;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.exception.InsufficientAvailabilityException;
import com.inventory.service.domain.exception.NotInitializedException;
import com.inventory.service.domain.model.Reservation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CreateReservationService} custom metrics (Micrometer).
 * Verifies that counters and timers are correctly registered on successful and
 * rejected reservation flows.
 */
class CreateReservationServiceMetricsTest {

    private FakeReservationRepository reservationRepository;
    private FakeReservationEventPublisher reservationEventPublisher;
    private FakeEventPublisherPort eventPublisherPort;
    private FakeAvailabilityRepository availabilityRepository;
    private SimpleMeterRegistry meterRegistry;
    private CreateReservationService service;

    @BeforeEach
    void setUp() {
        reservationRepository = new FakeReservationRepository();
        reservationEventPublisher = new FakeReservationEventPublisher();
        eventPublisherPort = new FakeEventPublisherPort();
        availabilityRepository = new FakeAvailabilityRepository();
        meterRegistry = new SimpleMeterRegistry();
        service = new CreateReservationService(
                reservationRepository, reservationEventPublisher, eventPublisherPort,
                availabilityRepository, meterRegistry);
    }

    @Test
    void createdCounterIncrementsOnSuccessfulReservation() {
        Reservation pending = new Reservation(null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        Reservation saved = new Reservation(99L, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        reservationRepository.resultToReturn = saved;

        Reservation result = service.createReservation(pending).block();

        assertThat(result).isSameAs(saved);
        assertThat(counterValue("reservations.created.total", "eventId", "10", "sectionId", "VIP", "status", Reservation.STATUS_PENDING))
                .isEqualTo(1.0);
    }

    @Test
    void rejectedCounterIncrementsOnInsufficientAvailability() {
        Reservation pending = new Reservation(null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        availabilityRepository.outcomeToReturn = ReservationOutcome.INSUFFICIENT_AVAILABILITY;

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(InsufficientAvailabilityException.class);

        assertThat(counterValue("reservations.rejected.total", "reason", "insufficient_availability"))
                .isEqualTo(1.0);
    }

    @Test
    void rejectedCounterIncrementsOnNotInitialized() {
        Reservation pending = new Reservation(null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        availabilityRepository.outcomeToReturn = ReservationOutcome.NOT_INITIALIZED;

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(NotInitializedException.class);

        assertThat(counterValue("reservations.rejected.total", "reason", "not_initialized"))
                .isEqualTo(1.0);
    }

    @Test
    void latencyTimerRecordsOnSuccess() {
        Reservation pending = new Reservation(null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        Reservation saved = new Reservation(99L, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        reservationRepository.resultToReturn = saved;

        service.createReservation(pending).block();

        assertThat(meterRegistry.find("reservations.latency").timer().count()).isEqualTo(1.0);
    }

    @Test
    void latencyTimerRecordsOnError() {
        Reservation pending = new Reservation(null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        reservationRepository.errorToReturn = new RuntimeException("db down");

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(RuntimeException.class);

        assertThat(meterRegistry.find("reservations.latency").timer().count()).isEqualTo(1.0);
    }

    private double counterValue(String name, String... tagKeyValue) {
        io.micrometer.core.instrument.Counter counter = meterRegistry.find(name).tags(tagKeyValue).counter();
        if (counter == null) {
            return 0.0;
        }
        return counter.count();
    }

    // ---- Fakes ----

    private static final class FakeReservationRepository implements ReservationRepository {
        private Reservation resultToReturn;
        private RuntimeException errorToReturn;
        private Reservation savedReservation;

        @Override
        public Mono<Reservation> save(Reservation reservation) {
            this.savedReservation = reservation;
            if (errorToReturn != null) {
                return Mono.error(errorToReturn);
            }
            return Mono.just(resultToReturn);
        }
    }

    private static final class FakeReservationEventPublisher implements ReservationEventPublisher {
        private RuntimeException errorToReturn;
        private Reservation publishedReservation;

        @Override
        public Mono<Void> publish(Reservation reservation) {
            this.publishedReservation = reservation;
            if (errorToReturn != null) {
                return Mono.error(errorToReturn);
            }
            return Mono.empty();
        }
    }

    private static final class FakeEventPublisherPort implements EventPublisherPort {
        private RuntimeException errorToReturn;
        private com.inventory.service.application.domain.event.ReservationConfirmedEvent publishedEvent;
        private boolean publishFailed;

        @Override
        public Mono<Void> publishReservationConfirmed(com.inventory.service.application.domain.event.ReservationConfirmedEvent event) {
            if (errorToReturn != null) {
                this.publishFailed = true;
                return Mono.error(errorToReturn);
            }
            this.publishedEvent = event;
            this.publishFailed = false;
            return Mono.empty();
        }
    }

    private static final class FakeAvailabilityRepository implements AvailabilityRepository {
        private ReservationOutcome outcomeToReturn = ReservationOutcome.RESERVED;
        private Integer reservedQuantity;
        private final java.util.List<Integer> releasedQuantities = new java.util.ArrayList<>();

        @Override
        public Mono<InitializeOutcome> initialize(Long eventId, String sectionId, Integer capacity) {
            return Mono.just(InitializeOutcome.INITIALIZED);
        }

        @Override
        public Mono<ReservationOutcome> tryReserve(Long eventId, String sectionId, Integer quantity) {
            if (outcomeToReturn == ReservationOutcome.RESERVED) {
                this.reservedQuantity = quantity;
            }
            return Mono.just(outcomeToReturn);
        }

        @Override
        public Mono<ReleaseOutcome> release(Long eventId, String sectionId, Integer quantity) {
            this.releasedQuantities.add(quantity);
            return Mono.just(ReleaseOutcome.RELEASED);
        }

        @Override
        public Mono<com.inventory.service.domain.model.Availability> getAvailability(Long eventId) {
            return Mono.empty();
        }
    }
}