package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Ticket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TicketRepository {

    Mono<Ticket> save(Ticket ticket);

    Flux<Ticket> findByUserId(String userId);

    Mono<Ticket> findByIdAndUserId(Long id, String userId);
}
