package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.usecase.ReservationService;
import com.inventory.service.infrastructure.adapter.in.controller.dto.CreateReservationRequest;
import com.inventory.service.infrastructure.adapter.in.controller.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reservas")
public class ReservationsController {

    private final ReservationService reservationService;

    public ReservationsController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('COMPRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ReservationResponse> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return reservationService.create(userId, idempotencyKey, request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('COMPRADOR')")
    public Mono<ReservationResponse> get(@PathVariable Long id,
                                         @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return reservationService.get(id, userId);
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('COMPRADOR')")
    public Mono<Page<ReservationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return reservationService.list(userId, page, size);
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('COMPRADOR')")
    public Mono<ReservationResponse> confirm(
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return reservationService.confirm(id, userId, idempotencyKey);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COMPRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> cancel(@PathVariable Long id,
                            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : null;
        return reservationService.cancel(id, userId);
    }
}
