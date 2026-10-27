package com.inventory.service.infrastructure.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;

import io.r2dbc.spi.Connection;
import java.time.Duration;

/**
 * Reactive health indicator for PostgreSQL R2DBC connectivity.
 * Executes a lightweight {@code SELECT 1} query and reports UP/DOWN accordingly.
 */
@Component
public class PostgreSQLHealthIndicator implements ReactiveHealthIndicator {

    private final ConnectionFactory connectionFactory;

    public PostgreSQLHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Mono<Health> health() {
        return Mono.from(connectionFactory.create())
                .timeout(Duration.ofSeconds(2))
                .flatMap(conn -> Mono.from(conn.createStatement("SELECT 1").execute())
                        .timeout(Duration.ofSeconds(2))
                        .doFinally(signal -> conn.close())
                        .map(result -> Health.up()
                                .withDetail("postgresql", "connected")
                                .withDetail("query", "SELECT 1")
                                .build()))
                .defaultIfEmpty(Health.down().withDetail("postgresql", "no connection").build())
                .onErrorResume(throwable -> Mono.just(
                        Health.down(throwable)
                                .withDetail("postgresql", "disconnected")
                                .withDetail("error", throwable.getMessage())
                                .build()));
    }
}