package com.inventory.service.infrastructure.adapter.in.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReservationRequest(
    @NotBlank(message = "eventId is required")
    String eventId,

    @NotBlank(message = "localityId is required")
    String localityId,

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be greater than zero")
    @Max(value = 6, message = "quantity must not exceed 6")
    Integer quantity
) {}
