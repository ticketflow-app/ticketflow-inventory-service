package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.exception.InsufficientAvailabilityException;
import com.inventory.service.application.exception.ReservationPersistenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps application exceptions to HTTP responses.
 *
 * <ul>
 *   <li>{@link InsufficientAvailabilityException} → 409 Conflict (CU-06 FA-01)</li>
 *   <li>{@link ReservationPersistenceException} → 503 Service Unavailable (CU-06 FA-03)</li>
 *   <li>{@link IllegalArgumentException} → 400 Bad Request</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientAvailabilityException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientAvailability(
            InsufficientAvailabilityException exception) {
        return build(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(ReservationPersistenceException.class)
    public ResponseEntity<Map<String, Object>> handleReservationPersistence(
            ReservationPersistenceException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, exception);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, exception);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", exception.getMessage());
        return ResponseEntity.status(status).body(body);
    }
}
