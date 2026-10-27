package com.inventory.service.infrastructure.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;

import io.r2dbc.spi.Connection;

/**
 * Combined reactive health indicator for infrastructure components.
 * Verifies Redis connectivity via PING and PostgreSQL R2DBC via SELECT 1.
 */
@Component
public class InfrastructureReactiveHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ConnectionFactory connectionFactory;

    public InfrastructureReactiveHealthIndicator(ReactiveStringRedisTemplate redisTemplate,
                                                 ConnectionFactory connectionFactory) {
        this.redisTemplate = redisTemplate;
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Mono<Health> health() {
        Mono<Boolean> redisOk = redisTemplate.execute(conn -> conn.ping())
                .next()
                .map(r -> true)
                .timeout(Duration.ofSeconds(2))
                .defaultIfEmpty(false)
                .onErrorResume(t -> Mono.just(false));

        Mono<Boolean> pgOk = Mono.from(connectionFactory.create())
                .timeout(Duration.ofSeconds(2))
                .flatMap(conn -> Mono.from(conn.createStatement("SELECT 1").execute())
                        .timeout(Duration.ofSeconds(2))
                        .doFinally(sig -> conn.close())
                        .map(r -> true))
                .defaultIfEmpty(false)
                .onErrorResume(t -> Mono.just(false));

        return Mono.zip(redisOk, pgOk)
                .timeout(Duration.ofSeconds(2))
                .map(tuple -> {
                    boolean redis = tuple.getT1();
                    boolean pg = tuple.getT2();
                    if (redis && pg) {
                        return Health.up()
                                .withDetail("redis", "connected")
                                .withDetail("postgresql", "connected")
                                .build();
                    }
                    Health.Builder builder = Health.down();
                    if (!redis) {
                        builder.withDetail("redis", "disconnected");
                    }
                    if (!pg) {
                        builder.withDetail("postgresql", "disconnected");
                    }
                    return builder.build();
                })
                .onErrorResume(throwable -> Mono.just(
                        Health.down(throwable)
                                .withDetail("error", throwable.getMessage())
                                .build()));
    }
}