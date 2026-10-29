package com.inventory.service.infrastructure.adapter.in.controller.dto;

public record ErrorResponse(
    String code,
    String message
) {}
