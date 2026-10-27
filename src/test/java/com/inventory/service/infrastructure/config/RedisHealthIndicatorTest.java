package com.inventory.service.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.boot.health.contributor.Status;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link RedisHealthIndicator}.
 * Verifies UP when Redis responds to PING and DOWN when it fails.
 */
class RedisHealthIndicatorTest {

    @Test
    void healthReturnsUpWhenRedisResponds() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        given(template.execute(any(org.springframework.data.redis.core.ReactiveRedisCallback.class))).willReturn(Flux.just("PONG"));

        RedisHealthIndicator indicator = new RedisHealthIndicator(template);

        StepVerifier.create(indicator.health())
                .expectNextMatches(health -> health.getStatus() == Status.UP)
                .expectComplete()
                .verify();
    }

    @Test
    void healthReturnsDownWhenRedisFails() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        given(template.execute(any(org.springframework.data.redis.core.ReactiveRedisCallback.class)))
                .willReturn(Flux.error(new RuntimeException("connection refused")));

        RedisHealthIndicator indicator = new RedisHealthIndicator(template);

        StepVerifier.create(indicator.health())
                .expectNextMatches(health -> health.getStatus() == Status.DOWN)
                .expectComplete()
                .verify();
    }
}