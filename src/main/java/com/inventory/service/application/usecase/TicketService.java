package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.TicketRepository;
import com.inventory.service.domain.exception.AccessDeniedException;
import com.inventory.service.domain.exception.ResourceNotFoundException;
import com.inventory.service.domain.model.Ticket;
import com.inventory.service.infrastructure.adapter.in.controller.dto.TicketResponse;
import com.inventory.service.infrastructure.qr.QrGeneratorService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final QrGeneratorService qrGeneratorService;

    public TicketService(TicketRepository ticketRepository, QrGeneratorService qrGeneratorService) {
        this.ticketRepository = ticketRepository;
        this.qrGeneratorService = qrGeneratorService;
    }

    public Mono<org.springframework.data.domain.Page<TicketResponse>> list(String userId, int page, int size) {
        return ticketRepository.findByUserId(userId)
                .map(this::toResponse)
                .collectList()
                .map(list -> {
                    int start = Math.min(page * size, list.size());
                    int end = Math.min(start + size, list.size());
                    List<TicketResponse> content = list.subList(start, end);
                    return new org.springframework.data.domain.PageImpl<>(
                            content,
                            org.springframework.data.domain.PageRequest.of(page, size),
                            list.size());
                });
    }

    public Mono<TicketResponse> get(Long id, String userId) {
        return ticketRepository.findByIdAndUserId(id, userId)
                .map(this::toResponse)
                .switchIfEmpty(Mono.<TicketResponse>error(new ResourceNotFoundException("Ticket not found or access denied: " + id)));
    }

    public Mono<byte[]> qr(Long id, String userId) {
        return ticketRepository.findByIdAndUserId(id, userId)
                .map(ticket -> qrGeneratorService.generateQrImage("ticket:" + ticket.getId(), 250, 250))
                .switchIfEmpty(Mono.<byte[]>error(new ResourceNotFoundException("Ticket not found or access denied: " + id)));
    }

    private TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getReservationId(),
                ticket.getUserId(),
                ticket.getEventId(),
                ticket.getLocalityId(),
                ticket.getSeatNumber(),
                ticket.getIssuedAt());
    }
}
