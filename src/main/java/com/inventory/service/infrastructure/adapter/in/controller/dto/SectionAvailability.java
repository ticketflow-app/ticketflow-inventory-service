package com.inventory.service.infrastructure.adapter.in.controller.dto;

import java.util.List;

public record SectionAvailability(
    String sectionId,
    Integer availableQuantity
) {}
