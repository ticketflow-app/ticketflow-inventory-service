package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.ReservationOutcome;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.application.port.out.TicketRepository;
import com.inventory.service.domain.exception.AccessDeniedException;
import com.inventory.service.domain.exception.EventNotSellableException;
import com.inventory.service.domain.exception.InsufficientStockException;
import com.inventory.service.domain.exception.InvalidReservationStateTransitionException;
import com.inventory.service.domain.exception.ReservationExpiredException;
import com.inventory.service.domain.exception.ResourceNotFoundException;
import com.inventory.service.domain.model.Reservation;
import com.inventory.service.domain.model.Ticket;
import com.inventory.service.infrastructure.adapter.in.controller.dto.CreateReservationRequest;
import com.inventory.service.infrastructure.adapter.in.controller.dto.ReservationResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReservationService {

    private final AvailabilityRepository availabilityRepository;
    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;

    // Ownership is persisted on the reservation row (user_id).
    private final Map<Long, Instant> expiryByReservation = new ConcurrentHashMap<>();
    private final Map<String, Long> idempotencyToReservation = new ConcurrentHashMap<>();

    public ReservationService(AvailabilityRepository availabilityRepository,
                              ReservationRepository reservationRepository,
                              TicketRepository ticketRepository) {
        this.availabilityRepository = availabilityRepository;
        this.reservationRepository = reservationRepository;
        this.ticketRepository = ticketRepository;
    }
    public Mono<ReservationResponse> create(String userId, String idempotencyKey, CreateReservationRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("Idempotency-Key is required"));
        }
        Long existingId = idempotencyToReservation.get(idempotencyKey);
        if (existingId != null) {
            return reservationRepository.findById(existingId)
                    .switchIfEmpty(Mono.error(new ResourceNotFoundException("Reservation not found: " + existingId)))
                    .map(reservation -> toResponse(reservation, userId));
        }

        return availabilityRepository.tryReserve(request.eventId(), request.localityId(), request.quantity())
                .flatMap(outcome -> {
                    if (outcome == ReservationOutcome.INSUFFICIENT_AVAILABILITY) {
                        return Mono.error(new InsufficientStockException(
                                "Insufficient stock for eventId=" + request.eventId() + " sectionId=" + request.localityId()));
                    }
                    if (outcome == ReservationOutcome.NOT_INITIALIZED) {
                        return Mono.error(new EventNotSellableException(
                                "Event not sellable for eventId=" + request.eventId()));
                    }
                    Instant now = Instant.now();
                    Instant expiresAt = now.plusSeconds(600);
                    Reservation reservation = new Reservation(
                            null, userId, request.eventId(), request.localityId(), request.quantity(), Reservation.STATUS_PENDING);
                    return reservationRepository.save(reservation)
                            .doOnNext(saved -> {
                                if (saved.getId() != null) {
                                    expiryByReservation.put(saved.getId(), expiresAt);
                                    idempotencyToReservation.put(idempotencyKey, saved.getId());
                                }
                            })
                            .map(saved -> new ReservationResponse(
                                    saved.getId(),
                                    userId,
                                    saved.getEventId(),
                                    saved.getLocalityId(),
                                    saved.getQuantity(),
                                    saved.getStatus(),
                                    600L,
                                    expiresAt));
                });
    }

    public Mono<ReservationResponse> get(Long id, String userId) {
        return reservationRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Reservation not found: " + id)))
                .flatMap(reservation -> {
                    String owner = reservation.getUserId();
                    if (owner != null && !owner.equals(userId)) {
                        return Mono.error(new AccessDeniedException("Access denied to reservation: " + id));
                    }
                    Instant expiresAt = expiryByReservation.get(id);
                    long remaining = 0L;
                    if (expiresAt != null) {
                        remaining = Math.max(0L, Duration.between(Instant.now(), expiresAt).getSeconds());
                    }
                    return Mono.just(new ReservationResponse(
                            reservation.getId(),
                            userId,
                            reservation.getEventId(),
                            reservation.getLocalityId(),
                            reservation.getQuantity(),
                            reservation.getStatus(),
                            remaining,
                            expiresAt));
                });
    }

    public Mono<org.springframework.data.domain.Page<ReservationResponse>> list(String userId, int page, int size) {
        return reservationRepository.findByUserId(userId)
                .map(reservation -> toResponse(reservation, userId))
                .collectList()
                .map(list -> {
                    int start = Math.min(page * size, list.size());
                    int end = Math.min(start + size, list.size());
                    java.util.List<ReservationResponse> content = list.subList(start, end);
                    return new org.springframework.data.domain.PageImpl<>(
                            content,
                            org.springframework.data.domain.PageRequest.of(page, size),
                            list.size());
                });
    }

    public Mono<ReservationResponse> confirm(Long id, String userId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.error(new IllegalArgumentException("Idempotency-Key is required"));
        }
        return reservationRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Reservation not found: " + id)))
                .flatMap(reservation -> {
                    String owner = reservation.getUserId();
                    if (owner != null && !owner.equals(userId)) {
                        return Mono.error(new AccessDeniedException("Access denied to reservation: " + id));
                    }
                    Instant expiresAt = expiryByReservation.get(id);
                    if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
                        return Mono.error(new ReservationExpiredException("Reservation expired: " + id));
                    }
                    if (!Reservation.STATUS_PENDING.equals(reservation.getStatus())) {
                        return Mono.error(new InvalidReservationStateTransitionException(
                                "Only a pending reservation can be confirmed"));
                    }
                    Reservation confirmed = reservation.confirm();
                    return reservationRepository.save(confirmed)
                            .flatMap(saved -> {
                                int quantity = saved.getQuantity();
                                return Flux.range(1, quantity)
                                        .flatMap(i -> ticketRepository.save(new Ticket(
                                                null,
                                                saved.getId(),
                                                userId,
                                                saved.getEventId(),
                                                saved.getLocalityId(),
                                                "A" + i,
                                                Instant.now())))
                                        .then(Mono.just(new ReservationResponse(
                                                saved.getId(),
                                                userId,
                                                saved.getEventId(),
                                                saved.getLocalityId(),
                                                saved.getQuantity(),
                                                saved.getStatus(),
                                                0L,
                                                expiresAt)));
                            });
                });
    }

    public Mono<Void> cancel(Long id, String userId) {
        return reservationRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Reservation not found: " + id)))
                .flatMap(reservation -> {
                    String owner = reservation.getUserId();
                    if (owner != null && !owner.equals(userId)) {
                        return Mono.error(new AccessDeniedException("Access denied to reservation: " + id));
                    }
                    if (!Reservation.STATUS_PENDING.equals(reservation.getStatus())) {
                        return Mono.error(new InvalidReservationStateTransitionException(
                                "Only a pending reservation can be cancelled"));
                    }
                    return availabilityRepository.release(reservation.getEventId(), reservation.getLocalityId(), reservation.getQuantity())
                            .then(reservationRepository.save(reservation.cancel()))
                            .then();
                });
    }

    private ReservationResponse toResponse(Reservation reservation, String userId) {
        String ownerId = reservation.getUserId() != null ? reservation.getUserId() : userId;
        Instant expiresAt = expiryByReservation.get(reservation.getId());
        long remaining = 0L;
        if (expiresAt != null) {
            remaining = Math.max(0L, Duration.between(Instant.now(), expiresAt).getSeconds());
        }
        return new ReservationResponse(
                reservation.getId(),
                ownerId,
                reservation.getEventId(),
                reservation.getLocalityId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                remaining,
                expiresAt);
    }

}
