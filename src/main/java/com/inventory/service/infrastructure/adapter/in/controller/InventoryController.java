package com.inventory.service.infrastructure.adapter.in.controller;

import com.inventory.service.application.usecase.AdjustStockService;
import com.inventory.service.application.usecase.GetEventAvailabilityService;
import com.inventory.service.infrastructure.adapter.in.controller.dto.EventAvailabilityResponse;
import com.inventory.service.infrastructure.adapter.in.controller.dto.StockAdjustmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/inventario")
public class InventoryController {

    private final GetEventAvailabilityService getEventAvailabilityService;
    private final AdjustStockService adjustStockService;

    public InventoryController(GetEventAvailabilityService getEventAvailabilityService,
                               AdjustStockService adjustStockService) {
        this.getEventAvailabilityService = getEventAvailabilityService;
        this.adjustStockService = adjustStockService;
    }

    @GetMapping("/eventos/{eventoId}/disponibilidad")
    public Mono<EventAvailabilityResponse> getAvailability(@PathVariable String eventoId) {
        return getEventAvailabilityService.getAvailability(eventoId);
    }

    @PostMapping("/eventos/{eventoId}/localidades/{localidadId}/ajuste")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> adjust(@PathVariable String eventoId,
                             @PathVariable String localidadId,
                             @Valid @RequestBody StockAdjustmentRequest request) {
        return adjustStockService.adjust(eventoId, localidadId, request.newAvailableQuantity());
    }
}
