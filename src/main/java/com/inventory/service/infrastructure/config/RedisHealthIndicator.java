package com.inventory.service.infrastructure.config;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;

/**
 * Reactive health indicator for Redis connectivity.
 * Executes a PING command against Redis and reports UP/DOWN accordingly.
 */
@Component
public class RedisHealthIndicator implements ReactiveHealthIndicator {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisHealthIndicator(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Health> health() {
        return redisTemplate.execute(connection -> connection.ping())
                .next()
                .timeout(Duration.ofSeconds(2))
                .map(result -> Health.up().withDetail("redis", "connected").build())
                .defaultIfEmpty(Health.down().withDetail("redis", "no response").build())
                .onErrorResume(throwable -> Mono.just(
                        Health.down(throwable)
                                .withDetail("redis", "disconnected")
                                .withDetail("error", throwable.getMessage())
                                .build()));
    }
}