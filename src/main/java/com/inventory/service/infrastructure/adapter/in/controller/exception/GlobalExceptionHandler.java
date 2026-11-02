package com.inventory.service.infrastructure.adapter.in.controller.exception;

import com.inventory.service.domain.exception.CatalogUnavailableException;
import com.inventory.service.domain.exception.EventNotSellableException;
import com.inventory.service.domain.exception.InsufficientAvailabilityException;
import com.inventory.service.domain.exception.InvalidReservationStateTransitionException;
import com.inventory.service.domain.exception.InsufficientStockException;
import com.inventory.service.domain.exception.NotInitializedException;
import com.inventory.service.domain.exception.ReservationExpiredException;
import com.inventory.service.domain.exception.ResourceNotFoundException;
import com.inventory.service.domain.exception.TooManyRequestsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global reactive exception handler for REST controllers.
 * Returns responses structured according to RFC 7807 (ProblemDetail).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientAvailabilityException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleInsufficientAvailability(
            InsufficientAvailabilityException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Insufficient Availability");
        problemDetail.setProperty("errorCode", "INSUFFICIENT_AVAILABILITY");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(NotInitializedException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleNotInitialized(
            NotInitializedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Availability Not Initialized");
        problemDetail.setProperty("errorCode", "NOT_INITIALIZED");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleValidation(
            WebExchangeBindException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("errorCode", "VALIDATION_ERROR");

        List<FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        problemDetail.setProperty("fieldErrors", fieldErrors);

        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("errorCode", "VALIDATION_ERROR");

        List<FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        problemDetail.setProperty("fieldErrors", fieldErrors);

        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleIllegalArgument(
            IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Invalid Argument");
        problemDetail.setProperty("errorCode", "INVALID_ARGUMENT");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleResourceNotFound(
            ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("errorCode", "RESOURCE_NOT_FOUND");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleInsufficientStock(
            InsufficientStockException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Insufficient Stock");
        problemDetail.setProperty("errorCode", "STOCK_INSUFFICIENT");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(EventNotSellableException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleEventNotSellable(
            EventNotSellableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Event Not Sellable");
        problemDetail.setProperty("errorCode", "EVENTO_NO_VENDIBLE");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(ReservationExpiredException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleReservationExpired(
            ReservationExpiredException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.GONE, ex.getMessage());
        problemDetail.setTitle("Reservation Expired");
        problemDetail.setProperty("errorCode", "RESERVA_EXPIRADA");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleTooManyRequests(
            TooManyRequestsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problemDetail.setTitle("Too Many Requests");
        problemDetail.setProperty("errorCode", "TOO_MANY_REQUESTS");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleCatalogUnavailable(
            CatalogUnavailableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problemDetail.setTitle("Catalog Service Unavailable");
        problemDetail.setProperty("errorCode", "CATALOG_UNAVAILABLE");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleSpringSecurityAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "No tienes permisos para acceder a este recurso");
        problemDetail.setTitle("Access Denied");
        problemDetail.setProperty("errorCode", "ACCESS_DENIED");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(InvalidReservationStateTransitionException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleInvalidTransition(
            InvalidReservationStateTransitionException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Invalid Reservation State Transition");
        problemDetail.setProperty("errorCode", "INVALID_STATE");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ProblemDetail>> handleGeneric(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("errorCode", "INTERNAL_ERROR");
        return Mono.just(ResponseEntity.of(problemDetail).build());
    }

    /**
     * Simple DTO to carry field-level validation error details.
     */
    public record FieldErrorDto(String field, String message) {}
}