package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.InitializeOutcome;
import com.inventory.service.application.port.out.ReleaseOutcome;
import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationOutcome;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.exception.InvalidAvailabilityException;
import com.inventory.service.domain.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateReservationServiceTest {

    private FakeReservationRepository reservationRepository;
    private FakeReservationEventPublisher reservationEventPublisher;
    private FakeAvailabilityRepository availabilityRepository;
    private CreateReservationService service;

    @BeforeEach
    void setUp() {
        reservationRepository = new FakeReservationRepository();
        reservationEventPublisher = new FakeReservationEventPublisher();
        availabilityRepository = new FakeAvailabilityRepository();
        service = new CreateReservationService(
                reservationRepository, reservationEventPublisher, availabilityRepository);
    }

    @Test
    void savesReservationAndPublishesItReturningTheSavedReservation() {
        Reservation pending = new Reservation(
                null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        Reservation saved = new Reservation(
                99L, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        reservationRepository.resultToReturn = saved;

        Reservation result = service.createReservation(pending).block();

        assertThat(availabilityRepository.reservedQuantity).isEqualTo(2);
        assertThat(reservationRepository.savedReservation).isSameAs(pending);
        assertThat(reservationEventPublisher.publishedReservation).isSameAs(saved);
        assertThat(result).isSameAs(saved);
        assertThat(availabilityRepository.releasedQuantities).isEmpty();
    }

    @Test
    void propagatesErrorFromSaveAndDoesNotPublishCompensatingTheAvailability() {
        Reservation pending = new Reservation(
                null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        RuntimeException failure = new RuntimeException("persistence unavailable");
        reservationRepository.errorToReturn = failure;

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isSameAs(failure);

        assertThat(availabilityRepository.reservedQuantity).isEqualTo(2);
        assertThat(availabilityRepository.releasedQuantities).containsExactly(2);
        assertThat(reservationEventPublisher.publishedReservation).isNull();
    }

    @Test
    void doesNotCompensateWhenPublishFailsAfterSuccessfulSave() {
        // The reservation IS persisted; releasing the stock would open an overselling window.
        Reservation pending = new Reservation(
                null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        Reservation saved = new Reservation(
                99L, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        RuntimeException failure = new RuntimeException("kafka unavailable");
        reservationRepository.resultToReturn = saved;
        reservationEventPublisher.errorToReturn = failure;

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isSameAs(failure);

        assertThat(availabilityRepository.releasedQuantities).isEmpty();
    }

    @Test
    void rejectsReservationWhenAvailabilityIsInsufficientWithoutPersisting() {
        Reservation pending = new Reservation(
                null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        availabilityRepository.outcomeToReturn = ReservationOutcome.INSUFFICIENT_AVAILABILITY;

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(InvalidAvailabilityException.class)
                .hasMessageContaining("Insufficient availability");

        assertThat(reservationRepository.savedReservation).isNull();
        assertThat(reservationEventPublisher.publishedReservation).isNull();
        assertThat(availabilityRepository.releasedQuantities).isEmpty();
    }

    @Test
    void rejectsReservationWhenAvailabilityIsNotInitializedWithoutPersisting() {
        Reservation pending = new Reservation(
                null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        availabilityRepository.outcomeToReturn = ReservationOutcome.NOT_INITIALIZED;

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(InvalidAvailabilityException.class)
                .hasMessageContaining("not initialized");

        assertThat(reservationRepository.savedReservation).isNull();
        assertThat(reservationEventPublisher.publishedReservation).isNull();
        assertThat(availabilityRepository.releasedQuantities).isEmpty();
    }

    @Test
    void propagatesErrorFromPublish() {
        Reservation pending = new Reservation(
                null, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        Reservation saved = new Reservation(
                99L, 10L, "VIP", 2, Reservation.STATUS_PENDING);
        RuntimeException failure = new RuntimeException("kafka unavailable");
        reservationRepository.resultToReturn = saved;
        reservationEventPublisher.errorToReturn = failure;

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isSameAs(failure);
    }

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

    private static final class FakeAvailabilityRepository implements AvailabilityRepository {
        private ReservationOutcome outcomeToReturn = ReservationOutcome.RESERVED;
        private Integer reservedQuantity;
        private final List<Integer> releasedQuantities = new ArrayList<>();

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
