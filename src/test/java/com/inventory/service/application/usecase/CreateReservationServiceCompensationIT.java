package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.exception.InvalidAvailabilityException;
import com.inventory.service.domain.model.Reservation;
import com.inventory.service.infrastructure.adapter.out.availability.RedisAvailabilityAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for the DA-001 short-saga compensation against a real ephemeral Redis
 * (Testcontainers, DA-005): simulates a PostgreSQL write failure during a reservation and
 * verifies that the compensating action restores the stock in Redis, without publishing
 * the event, and that an insufficient stock reservation is rejected without persisting.
 */
@Testcontainers
class CreateReservationServiceCompensationIT {

    private static final long EVENT_ID = 909100L;
    private static final String SECTION = "VIP";

    @Container
    @SuppressWarnings("rawtypes")
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static ReactiveStringRedisTemplate template;
    private RedisAvailabilityAdapter adapter;
    private CreateReservationService service;
    private FailingReservationRepository reservationRepository;
    private RecordingEventPublisher eventPublisher;

    @BeforeAll
    static void setUpAll() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        template = new ReactiveStringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void tearDownAll() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        List<String> keys = template.keys("availability:" + EVENT_ID + ":*").collectList().block();
        if (keys != null) {
            keys.forEach(key -> template.delete(key).block());
        }
        adapter = new RedisAvailabilityAdapter(template);
        reservationRepository = new FailingReservationRepository();
        eventPublisher = new RecordingEventPublisher();
        service = new CreateReservationService(reservationRepository, eventPublisher, adapter);
    }

    private Integer readAvailability() {
        String value = template.opsForValue().get("availability:" + EVENT_ID + ":" + SECTION).block();
        return value == null ? null : Integer.valueOf(value);
    }

    @Test
    @DisplayName("DB failure after a successful tryReserve restores the stock in Redis (DA-001 compensation)")
    void compensatesRedisStockWhenPersistenceFails() {
        adapter.initialize(EVENT_ID, SECTION, 10).block();
        assertThat(readAvailability()).isEqualTo(10);

        reservationRepository.failure = new RuntimeException("postgres connection refused");
        Reservation pending = new Reservation(null, EVENT_ID, SECTION, 3, Reservation.STATUS_PENDING);

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isSameAs(reservationRepository.failure);

        assertThat(readAvailability())
                .as("compensating release restored the stock to its initial value")
                .isEqualTo(10);
        assertThat(eventPublisher.published)
                .as("no event is published when persistence fails")
                .isFalse();
    }

    @Test
    @DisplayName("Successful reservation decrements Redis and publishes the event")
    void reservesAndPersistsWhenEverythingSucceeds() {
        adapter.initialize(EVENT_ID, SECTION, 10).block();
        reservationRepository.result = new Reservation(7L, EVENT_ID, SECTION, 3, Reservation.STATUS_PENDING);
        Reservation pending = new Reservation(null, EVENT_ID, SECTION, 3, Reservation.STATUS_PENDING);

        Reservation saved = service.createReservation(pending).block();

        assertThat(saved.getId()).isEqualTo(7L);
        assertThat(readAvailability()).as("stock decremented once").isEqualTo(7);
        assertThat(eventPublisher.published).isTrue();
    }

    @Test
    @DisplayName("Insufficient stock is rejected without persisting or publishing")
    void rejectsInsufficientStockWithoutSideEffects() {
        adapter.initialize(EVENT_ID, SECTION, 2).block();
        Reservation pending = new Reservation(null, EVENT_ID, SECTION, 5, Reservation.STATUS_PENDING);

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(InvalidAvailabilityException.class);

        assertThat(readAvailability()).as("stock untouched").isEqualTo(2);
        assertThat(reservationRepository.saveCalled).isFalse();
        assertThat(eventPublisher.published).isFalse();
    }

    private static final class FailingReservationRepository implements ReservationRepository {
        private RuntimeException failure;
        private Reservation result;
        private boolean saveCalled;

        @Override
        public Mono<Reservation> save(Reservation reservation) {
            this.saveCalled = true;
            if (failure != null) {
                return Mono.error(failure);
            }
            return Mono.just(result);
        }
    }

    private static final class RecordingEventPublisher implements ReservationEventPublisher {
        private boolean published;

        @Override
        public Mono<Void> publish(Reservation reservation) {
            this.published = true;
            return Mono.empty();
        }
    }
}