package com.inventory.service.application.port.in.dto;

/**
 * Inbound payload to configure the capacity of a locality (HU-11).
 */
public record ConfigureCapacityRequest(Integer capacity) {
}
