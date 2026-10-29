package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.usecase.TicketService;
import com.inventory.service.infrastructure.adapter.in.controller.dto.TicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/boletos")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/mios")
    @PreAuthorize("hasRole('COMPRADOR')")
    public Mono<Page<TicketResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ticketService.list(userId, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COMPRADOR')")
    public Mono<TicketResponse> get(@PathVariable Long id,
                                   @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ticketService.get(id, userId);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('COMPRADOR')")
    public Mono<byte[]> qr(@PathVariable Long id,
                           @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return ticketService.qr(id, userId);
    }
}
