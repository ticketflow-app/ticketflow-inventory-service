package com.inventory.service.infrastructure.adapter.out.persistence;

import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.Reservation;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class ReservationPersistenceAdapter implements ReservationRepository {

    private final ConnectionFactory connectionFactory;

    public ReservationPersistenceAdapter(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Mono<Reservation> save(Reservation reservation) {

        return Mono.from(connectionFactory.create())
                .flatMap(connection ->
                        Mono.from(connection.createStatement(
                                        """
                                        INSERT INTO reservations
                                        (event_id, locality, quantity, status)
                                        VALUES ($1, $2, $3, $4)
                                        RETURNING id
                                        """)
                                .bind("$1", reservation.getEventId())
                                .bind("$2", reservation.getLocality())
                                .bind("$3", reservation.getQuantity())
                                .bind("$4", reservation.getStatus())
                                .execute())
                                .flatMap(result ->
                                        Mono.from(result.map((row, metadata) ->
                                                row.get("id", Long.class)
                                        ))
                                )
                                .map(id -> new Reservation(
                                        id,
                                        reservation.getEventId(),
                                        reservation.getLocality(),
                                        reservation.getQuantity(),
                                        reservation.getStatus()
                                ))
                                .doFinally(signal ->
                                        Mono.from(connection.close()).subscribe())
                );
    }
}