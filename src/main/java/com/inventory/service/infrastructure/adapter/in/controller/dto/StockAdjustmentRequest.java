package com.inventory.service.infrastructure.adapter.in.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
    @NotNull(message = "newAvailableQuantity is required")
    @Min(value = 0, message = "newAvailableQuantity must not be negative")
    Integer newAvailableQuantity
) {}
