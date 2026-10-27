package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.exception.InvalidAvailabilityException;
import com.inventory.service.domain.model.Reservation;
import com.inventory.service.infrastructure.adapter.out.availability.RedisAvailabilityAdapter;
import com.inventory.service.infrastructure.adapter.out.persistence.ReservationPersistenceAdapter;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for the DA-001 short-saga with a real PostgreSQL (R2DBC) and real Redis
 * (Testcontainers, DA-005). Validates:
 * <ul>
 *   <li>Happy path: reserve in Redis → persist in PostgreSQL → publish event.</li>
 *   <li>Compensation: PostgreSQL failure → release stock back in Redis.</li>
 *   <li>Rejection: insufficient stock in Redis → no write to PostgreSQL.</li>
 * </ul>
 */
@Testcontainers
class CreateReservationServicePostgresIT {

    private static final long EVENT_ID = 909200L;
    private static final String SECTION = "VIP";

    @Container
    @SuppressWarnings("rawtypes")
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventory")
            .withUsername("postgres")
            .withPassword("postgres");

    private static LettuceConnectionFactory redisConnectionFactory;
    private static ReactiveStringRedisTemplate redisTemplate;
    private static DatabaseClient databaseClient;

    private AvailabilityRepository availabilityRepository;
    private ReservationRepository reservationRepository;
    private ReservationEventPublisher eventPublisher;
    private CreateReservationService service;

    @BeforeAll
    static void setUpAll() {
        redisConnectionFactory = new LettuceConnectionFactory(
                REDIS.getHost(), REDIS.getMappedPort(6379));
        redisConnectionFactory.afterPropertiesSet();
        redisTemplate = new ReactiveStringRedisTemplate(redisConnectionFactory);

        ConnectionFactoryOptions pgOptions = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                .option(ConnectionFactoryOptions.HOST, POSTGRES.getHost())
                .option(ConnectionFactoryOptions.PORT, POSTGRES.getMappedPort(5432))
                .option(ConnectionFactoryOptions.DATABASE, POSTGRES.getDatabaseName())
                .option(ConnectionFactoryOptions.USER, POSTGRES.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, POSTGRES.getPassword())
                .build();
        ConnectionFactory pgConnectionFactory = ConnectionFactories.get(pgOptions);
        databaseClient = DatabaseClient.create(pgConnectionFactory);

        databaseClient.sql("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    event_id BIGINT NOT NULL,
                    section_id VARCHAR(255) NOT NULL,
                    quantity INTEGER NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                CREATE INDEX IF NOT EXISTS idx_reservations_event_section ON reservations(event_id, section_id);
                CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
                """).then().block();
    }

    @AfterAll
    static void tearDownAll() {
        if (redisConnectionFactory != null) {
            redisConnectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        List<String> keys = redisTemplate.keys("availability:" + EVENT_ID + ":*")
                .collectList().block();
        if (keys != null) {
            keys.forEach(key -> redisTemplate.delete(key).block());
        }

        databaseClient.sql("DELETE FROM reservations WHERE event_id = $1")
                .bind("$1", EVENT_ID)
                .then().block();

        availabilityRepository = new RedisAvailabilityAdapter(redisTemplate, io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults());
        reservationRepository = new ReservationPersistenceAdapter(databaseClient);
        eventPublisher = new RecordingEventPublisher();
        service = new CreateReservationService(
                reservationRepository, eventPublisher, e -> Mono.empty(), availabilityRepository, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    private Integer readRedisAvailability() {
        String value = redisTemplate.opsForValue()
                .get("availability:" + EVENT_ID + ":" + SECTION).block();
        return value == null ? null : Integer.valueOf(value);
    }

    private Long countReservationsInDb() {
        return databaseClient.sql("SELECT COUNT(*) FROM reservations WHERE event_id = $1")
                .bind("$1", EVENT_ID)
                .map((row, metadata) -> row.get("count", Long.class))
                .first().block();
    }

    @Test
    @DisplayName("Happy path: Redis RESERVED → PostgreSQL PENDING → event published")
    void happyPathReserveAndPersist() {
        availabilityRepository.initialize(EVENT_ID, SECTION, 10).block();
        assertThat(readRedisAvailability()).isEqualTo(10);

        Reservation pending = new Reservation(null, EVENT_ID, SECTION, 3, Reservation.STATUS_PENDING);
        Reservation saved = service.createReservation(pending).block();

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getLocality()).isEqualTo(SECTION);
        assertThat(saved.getQuantity()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(Reservation.STATUS_PENDING);

        assertThat(readRedisAvailability()).as("stock decremented in Redis").isEqualTo(7);
        assertThat(countReservationsInDb()).as("one reservation persisted").isEqualTo(1L);
        assertThat(((RecordingEventPublisher) eventPublisher).isPublished())
                .as("event published after successful persistence").isTrue();
    }

    @Test
    @DisplayName("Compensation: PostgreSQL failure after Redis RESERVED → release stock in Redis")
    void compensatesRedisStockWhenPersistenceFails() {
        availabilityRepository.initialize(EVENT_ID, SECTION, 10).block();
        assertThat(readRedisAvailability()).isEqualTo(10);

        reservationRepository = new FailingReservationRepository(new RuntimeException("postgres connection refused"));
        service = new CreateReservationService(
                reservationRepository, eventPublisher, e -> Mono.empty(), availabilityRepository, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

        Reservation pending = new Reservation(null, EVENT_ID, SECTION, 3, Reservation.STATUS_PENDING);

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("postgres connection refused");

        assertThat(readRedisAvailability())
                .as("compensating release restored the stock to its initial value")
                .isEqualTo(10);
        assertThat(countReservationsInDb()).as("no reservation persisted").isEqualTo(0L);
        assertThat(((RecordingEventPublisher) eventPublisher).isPublished())
                .as("no event published when persistence fails").isFalse();
    }

    @Test
    @DisplayName("Rejection: INSUFFICIENT_AVAILABILITY in Redis → no write to PostgreSQL")
    void rejectsInsufficientStockWithoutSideEffects() {
        availabilityRepository.initialize(EVENT_ID, SECTION, 2).block();
        assertThat(readRedisAvailability()).isEqualTo(2);

        Reservation pending = new Reservation(null, EVENT_ID, SECTION, 5, Reservation.STATUS_PENDING);

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(InvalidAvailabilityException.class)
                .hasMessageContaining("Insufficient availability");

        assertThat(readRedisAvailability()).as("stock untouched").isEqualTo(2);
        assertThat(countReservationsInDb()).as("no reservation persisted").isEqualTo(0L);
        assertThat(((RecordingEventPublisher) eventPublisher).isPublished())
                .as("no event published").isFalse();
    }

    @Test
    @DisplayName("Rejection: NOT_INITIALIZED in Redis → no write to PostgreSQL")
    void rejectsNotInitializedWithoutSideEffects() {
        Reservation pending = new Reservation(null, EVENT_ID, SECTION, 2, Reservation.STATUS_PENDING);

        assertThatThrownBy(() -> service.createReservation(pending).block())
                .isInstanceOf(InvalidAvailabilityException.class)
                .hasMessageContaining("not initialized");

        assertThat(countReservationsInDb()).as("no reservation persisted").isEqualTo(0L);
        assertThat(((RecordingEventPublisher) eventPublisher).isPublished())
                .as("no event published").isFalse();
    }

    private static final class RecordingEventPublisher implements ReservationEventPublisher {
        private boolean published;

        @Override
        public Mono<Void> publish(Reservation reservation) {
            this.published = true;
            return Mono.empty();
        }

        boolean isPublished() {
            return published;
        }
    }

    private static final class FailingReservationRepository implements ReservationRepository {
        private final RuntimeException failure;

        FailingReservationRepository(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public Mono<Reservation> save(Reservation reservation) {
            return Mono.error(failure);
        }
    }
}