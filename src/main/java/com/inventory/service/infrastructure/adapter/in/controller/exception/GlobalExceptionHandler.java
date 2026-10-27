package com.inventory.service.infrastructure.adapter.in.controller.exception;

import com.inventory.service.domain.exception.InsufficientAvailabilityException;
import com.inventory.service.domain.exception.NotInitializedException;
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