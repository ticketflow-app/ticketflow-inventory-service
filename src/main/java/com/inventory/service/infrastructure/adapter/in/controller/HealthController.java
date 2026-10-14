package com.inventory.service.infrastructure.adapter.in.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Service liveness probe")
public class HealthController {

    @Operation(summary = "Health check", description = "Returns UP when the service is running.")
    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}
