package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.port.in.ConfigureCapacityUseCase;
import com.inventory.service.application.port.in.dto.ConfigureCapacityRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Capacity", description = "Locality capacity configuration (HU-11)")
public class CapacityController {

    private final ConfigureCapacityUseCase configureCapacityUseCase;

    public CapacityController(ConfigureCapacityUseCase configureCapacityUseCase) {
        this.configureCapacityUseCase = configureCapacityUseCase;
    }

    @Operation(summary = "Configure locality capacity",
            description = "Registers the total capacity of a locality and (re)initializes its availability counter in Redis.")
    @PutMapping("/events/{eventId}/localities/{locality}/capacity")
    public ResponseEntity<Void> configureCapacity(
            @PathVariable Long eventId,
            @PathVariable String locality,
            @RequestBody ConfigureCapacityRequest request) {
        int capacity = request.capacity() == null ? -1 : request.capacity();
        configureCapacityUseCase.configure(eventId, locality, capacity);
        return ResponseEntity.ok().build();
    }
}
