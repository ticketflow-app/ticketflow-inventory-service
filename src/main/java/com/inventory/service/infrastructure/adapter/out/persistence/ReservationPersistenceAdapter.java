package com.inventory.service.infrastructure.adapter.out.persistence;

import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.Reservation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Persistence adapter for reservations using Spring Data R2DBC {@link DatabaseClient}.
 * Fully reactive: no JDBC, no JPA, no Hibernate, no blocking I/O.
 */
@Repository
public class ReservationPersistenceAdapter implements ReservationRepository {

    private final DatabaseClient databaseClient;

    public ReservationPersistenceAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Reservation> save(Reservation reservation) {
        return databaseClient.sql("""
                INSERT INTO reservations (event_id, section_id, quantity, status, created_at)
                VALUES ($1, $2, $3, $4, $5)
                RETURNING id, event_id, section_id, quantity, status, created_at
                """)
                .bind("$1", reservation.getEventId())
                .bind("$2", reservation.getLocality())
                .bind("$3", reservation.getQuantity())
                .bind("$4", reservation.getStatus())
                .bind("$5", Instant.now())
                .map((row, metadata) -> new Reservation(
                        row.get("id", Long.class),
                        row.get("event_id", Long.class),
                        row.get("section_id", String.class),
                        row.get("quantity", Integer.class),
                        row.get("status", String.class)
                ))
                .first()
                .timeout(Duration.ofSeconds(5));
    }
}