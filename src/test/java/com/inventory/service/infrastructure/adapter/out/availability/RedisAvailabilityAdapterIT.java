package com.inventory.service.infrastructure.adapter.out.availability;

import com.inventory.service.application.port.out.InitializeOutcome;
import com.inventory.service.application.port.out.ReleaseOutcome;
import com.inventory.service.application.port.out.ReservationOutcome;
import com.inventory.service.domain.model.Availability;
import com.inventory.service.domain.model.LocalityAvailability;
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
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link RedisAvailabilityAdapter} against an ephemeral Redis
 * container managed by Testcontainers (DA-005): no manual {@code inventory-redis}
 * container is required anymore.
 *
 * <p>The class is named with the {@code IT} suffix so it is not part of the default
 * {@code mvnw test} run; it must be executed explicitly (e.g.
 * {@code mvnw test -Dtest=RedisAvailabilityAdapterIT}). Docker must be available.</p>
 */
@Testcontainers
class RedisAvailabilityAdapterIT {

    private static final long EVENT_ID = 909000L;
    private static final List<String> SECTIONS =
            List.of("VIP", "General", "C2", "C3", "C4", "C4Z", "C5", "C6", "C7", "CONC", "CINIT");

    @Container
    @SuppressWarnings("rawtypes")
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static ReactiveStringRedisTemplate template;
    private static RedisAvailabilityAdapter adapter;

    @BeforeAll
    static void setUpAll() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        template = new ReactiveStringRedisTemplate(connectionFactory);
        adapter = new RedisAvailabilityAdapter(template);
    }

    @AfterAll
    static void tearDownAll() {
        cleanup();
        connectionFactory.destroy();
    }

    @BeforeEach
    void cleanupBefore() {
        cleanup();
    }

    private static void cleanup() {
        for (String section : SECTIONS) {
            template.delete(availabilityKey(EVENT_ID, section)).block();
            template.delete(capacityKey(EVENT_ID, section)).block();
        }
    }

    private static String availabilityKey(long eventId, String sectionId) {
        return "availability:" + eventId + ":" + sectionId;
    }

    private static String capacityKey(long eventId, String sectionId) {
        return "availability:" + eventId + ":" + sectionId + ":capacity";
    }

    private static Integer readAvailability(String sectionId) {
        String value = template.opsForValue().get(availabilityKey(EVENT_ID, sectionId)).block();
        return value == null ? null : Integer.valueOf(value);
    }

    private static Integer readCapacity(String sectionId) {
        String value = template.opsForValue().get(capacityKey(EVENT_ID, sectionId)).block();
        return value == null ? null : Integer.valueOf(value);
    }

    @Test
    @DisplayName("C1 - concurrencia: K disponibles, M>K solicitudes => exactamente K RESERVED, disponibilidad final 0")
    void c1_concurrencyNoOversell() {
        int capacity = 10;
        int requests = 50;

        assertThat(adapter.initialize(EVENT_ID, "CONC", capacity).block())
                .isEqualTo(InitializeOutcome.INITIALIZED);

        List<ReservationOutcome> outcomes = Flux.range(1, requests)
                .flatMap(i -> adapter.tryReserve(EVENT_ID, "CONC", 1))
                .collectList()
                .block();

        long reserved = outcomes.stream().filter(o -> o == ReservationOutcome.RESERVED).count();
        long insufficient = outcomes.stream()
                .filter(o -> o == ReservationOutcome.INSUFFICIENT_AVAILABILITY).count();

        assertThat(reserved).as("reserved count").isEqualTo(capacity);
        assertThat(insufficient).as("insufficient count").isEqualTo(requests - capacity);
        assertThat(readAvailability("CONC")).as("final availability").isZero();
    }

    @Test
    @DisplayName("C2 - disponibilidad insuficiente rechaza y no altera el contador")
    void c2_insufficientAvailability() {
        adapter.initialize(EVENT_ID, "C2", 3).block();

        ReservationOutcome outcome = adapter.tryReserve(EVENT_ID, "C2", 5).block();

        assertThat(outcome).isEqualTo(ReservationOutcome.INSUFFICIENT_AVAILABILITY);
        assertThat(readAvailability("C2")).isEqualTo(3);
    }

    @Test
    @DisplayName("C3 - seccion no inicializada => NOT_INITIALIZED y no crea claves")
    void c3_notInitialized() {
        assertThat(adapter.tryReserve(EVENT_ID, "C3", 1).block())
                .isEqualTo(ReservationOutcome.NOT_INITIALIZED);
        assertThat(adapter.release(EVENT_ID, "C3", 1).block())
                .isEqualTo(ReleaseOutcome.NOT_INITIALIZED);

        assertThat(template.hasKey(availabilityKey(EVENT_ID, "C3")).block()).isFalse();
        assertThat(readAvailability("C3")).isNull();
    }

    @Test
    @DisplayName("C4 - inicializacion valida, incluida capacidad cero")
    void c4_initializeValid() {
        assertThat(adapter.initialize(EVENT_ID, "C4", 5).block())
                .isEqualTo(InitializeOutcome.INITIALIZED);
        assertThat(readAvailability("C4")).isEqualTo(5);
        assertThat(readCapacity("C4")).isEqualTo(5);

        assertThat(adapter.initialize(EVENT_ID, "C4Z", 0).block())
                .isEqualTo(InitializeOutcome.INITIALIZED);
        assertThat(readAvailability("C4Z")).isZero();
        assertThat(readCapacity("C4Z")).isZero();
    }

    @Test
    @DisplayName("C5 - segunda inicializacion no sobrescribe la disponibilidad en uso")
    void c5_initializeNonDestructive() {
        adapter.initialize(EVENT_ID, "C5", 5).block();
        adapter.tryReserve(EVENT_ID, "C5", 2).block();

        assertThat(adapter.initialize(EVENT_ID, "C5", 100).block())
                .isEqualTo(InitializeOutcome.ALREADY_INITIALIZED);

        assertThat(readAvailability("C5")).as("availability must not be reset").isEqualTo(3);
        assertThat(readCapacity("C5")).isEqualTo(5);
    }

    @Test
    @DisplayName("C6 - liberacion acotada a la capacidad inicial")
    void c6_releaseCapped() {
        adapter.initialize(EVENT_ID, "C6", 4).block();
        adapter.tryReserve(EVENT_ID, "C6", 4).block();

        assertThat(adapter.release(EVENT_ID, "C6", 10).block())
                .isEqualTo(ReleaseOutcome.RELEASED);
        assertThat(readAvailability("C6")).as("capped at capacity").isEqualTo(4);

        adapter.tryReserve(EVENT_ID, "C6", 1).block();
        assertThat(adapter.release(EVENT_ID, "C6", 1).block())
                .isEqualTo(ReleaseOutcome.RELEASED);
        assertThat(readAvailability("C6")).isEqualTo(4);
    }

    @Test
    @DisplayName("C7 - validaciones sin corromper el estado de Redis")
    void c7_validations() {
        assertThatThrownBy(() -> adapter.initialize(null, "C7", 1).block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.initialize(EVENT_ID, "  ", 1).block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.initialize(EVENT_ID, "C7", -1).block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.tryReserve(EVENT_ID, "C7", 0).block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.tryReserve(EVENT_ID, "C7", null).block())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.release(EVENT_ID, "C7", -2).block())
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(template.hasKey(availabilityKey(EVENT_ID, "C7")).block())
                .as("no key created by invalid calls").isFalse();
    }

    @Test
    @DisplayName("getAvailability conserva el contrato leyendo las secciones sembradas")
    void getAvailabilityCompatibility() {
        adapter.initialize(EVENT_ID, "VIP", 10).block();
        adapter.initialize(EVENT_ID, "General", 20).block();

        Availability availability = adapter.getAvailability(EVENT_ID).block();

        assertThat(availability.getEventId()).isEqualTo(EVENT_ID);
        assertThat(availability.getLocalities())
                .extracting(LocalityAvailability::getLocality)
                .containsExactly("VIP", "General");
        assertThat(availability.getLocalities())
                .extracting(LocalityAvailability::getAvailableTickets)
                .containsExactly(10, 20);
    }

    @Test
    @DisplayName("C6c - inicializaciones concurrentes: exactamente una INITIALIZED y disponibilidad = su capacidad")
    void c6c_concurrentInitializeOnlyOneWins() {
        // Distinct capacities so the single winner can be identified by the persisted value.
        List<Integer> capacities = List.of(101, 202, 303, 404, 505, 606, 707, 808);

        List<String> outcomes = Flux.range(0, capacities.size())
                .flatMap(i -> adapter.initialize(EVENT_ID, "CINIT", capacities.get(i))
                        .map(outcome -> outcome.name() + "@" + capacities.get(i)))
                .collectList()
                .block();

        long initialized = outcomes.stream().filter(r -> r.startsWith("INITIALIZED")).count();
        long alreadyInitialized = outcomes.stream().filter(r -> r.startsWith("ALREADY_INITIALIZED")).count();

        assertThat(initialized).as("exactly one INITIALIZED").isEqualTo(1);
        assertThat(alreadyInitialized).as("the rest ALREADY_INITIALIZED").isEqualTo(capacities.size() - 1);

        int winnerCapacity = outcomes.stream()
                .filter(r -> r.startsWith("INITIALIZED"))
                .map(r -> Integer.parseInt(r.substring(r.indexOf('@') + 1)))
                .findFirst()
                .orElseThrow();

        assertThat(readAvailability("CINIT"))
                .as("final availability equals the winning capacity").isEqualTo(winnerCapacity);
        assertThat(readCapacity("CINIT"))
                .as("capacity key equals the winning capacity").isEqualTo(winnerCapacity);
    }
}
