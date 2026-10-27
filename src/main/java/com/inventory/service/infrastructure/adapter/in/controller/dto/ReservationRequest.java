package com.inventory.service.infrastructure.adapter.in.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a reservation.
 */
public record ReservationRequest(
    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be greater than zero")
    Integer quantity
) {}