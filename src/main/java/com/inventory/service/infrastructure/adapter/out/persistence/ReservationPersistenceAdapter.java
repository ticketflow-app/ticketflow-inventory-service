package com.inventory.service.infrastructure.adapter.out.persistence;

import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.Reservation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Persistence adapter for reservations using Spring Data R2DBC {@link DatabaseClient}.
 * Fully reactive: no JDBC, no JPA, no Hibernate, no blocking I/O.
 */
@Repository
public class ReservationPersistenceAdapter implements ReservationRepository {

    private static final String SELECT_COLUMNS =
            "id, user_id, event_id, locality_id, quantity, status, created_at";

    private final DatabaseClient databaseClient;

    public ReservationPersistenceAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Reservation> save(Reservation reservation) {
        if (reservation.getId() == null) {
            return databaseClient.sql("""
                    INSERT INTO reservations (user_id, event_id, locality_id, quantity, status, created_at)
                    VALUES ($1, $2, $3, $4, $5, $6)
                    RETURNING %s
                    """.formatted(SELECT_COLUMNS))
                    .bind("$1", reservation.getUserId())
                    .bind("$2", reservation.getEventId())
                    .bind("$3", reservation.getLocalityId())
                    .bind("$4", reservation.getQuantity())
                    .bind("$5", reservation.getStatus())
                    .bind("$6", Instant.now())
                    .map((row, metadata) -> toDomain(row))
                    .first()
                    .timeout(Duration.ofSeconds(5));
        }
        return databaseClient.sql("""
                UPDATE reservations
                SET status = $2
                WHERE id = $1
                RETURNING %s
                """.formatted(SELECT_COLUMNS))
                .bind("$1", reservation.getId())
                .bind("$2", reservation.getStatus())
                .map((row, metadata) -> toDomain(row))
                .first()
                .timeout(Duration.ofSeconds(5));
    }

    @Override
    public Mono<Reservation> findById(Long id) {
        return databaseClient.sql("SELECT %s FROM reservations WHERE id = $1".formatted(SELECT_COLUMNS))
                .bind("$1", id)
                .map((row, metadata) -> toDomain(row))
                .first()
                .timeout(Duration.ofSeconds(5));
    }

    @Override
    public Flux<Reservation> findByUserId(String userId) {
        return databaseClient.sql("SELECT %s FROM reservations WHERE user_id = $1 ORDER BY id DESC"
                        .formatted(SELECT_COLUMNS))
                .bind("$1", userId)
                .map((row, metadata) -> toDomain(row))
                .all()
                .timeout(Duration.ofSeconds(5));
    }

    private Reservation toDomain(io.r2dbc.spi.Row row) {
        return new Reservation(
                row.get("id", Long.class),
                row.get("user_id", String.class),
                row.get("event_id", String.class),
                row.get("locality_id", String.class),
                row.get("quantity", Integer.class),
                row.get("status", String.class));
    }
}
