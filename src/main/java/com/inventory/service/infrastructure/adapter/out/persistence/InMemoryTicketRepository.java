package com.inventory.service.infrastructure.adapter.out.persistence;

import com.inventory.service.application.port.out.TicketRepository;
import com.inventory.service.domain.model.Ticket;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryTicketRepository implements TicketRepository {

    private final AtomicLong sequence = new AtomicLong(1L);
    private final ConcurrentHashMap<Long, Ticket> store = new ConcurrentHashMap<>();

    @Override
    public Mono<Ticket> save(Ticket ticket) {
        Long id = ticket.getId() != null ? ticket.getId() : sequence.getAndIncrement();
        Ticket saved = new Ticket(
                id,
                ticket.getReservationId(),
                ticket.getUserId(),
                ticket.getEventId(),
                ticket.getLocalityId(),
                ticket.getSeatNumber(),
                ticket.getIssuedAt());
        store.put(id, saved);
        return Mono.just(saved);
    }

    @Override
    public Flux<Ticket> findByUserId(String userId) {
        return Flux.fromIterable(store.values())
                .filter(ticket -> userId != null && userId.equals(ticket.getUserId()));
    }

    @Override
    public Mono<Ticket> findByIdAndUserId(Long id, String userId) {
        Ticket ticket = store.get(id);
        if (ticket == null || !java.util.Objects.equals(ticket.getUserId(), userId)) {
            return Mono.empty();
        }
        return Mono.just(ticket);
    }
}
