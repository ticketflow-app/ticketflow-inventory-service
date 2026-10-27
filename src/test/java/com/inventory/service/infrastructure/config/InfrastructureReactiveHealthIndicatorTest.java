package com.inventory.service.infrastructure.config;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.boot.health.contributor.Status;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class InfrastructureReactiveHealthIndicatorTest {

    @Test
    void healthReturnsUpWhenBothRespond() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        given(redisTemplate.execute(any(org.springframework.data.redis.core.ReactiveRedisCallback.class))).willReturn(Flux.just("PONG"));

        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        Result result = mock(Result.class);
        doReturn(Mono.<Connection>just(conn)).when(factory).create();
        given(conn.createStatement("SELECT 1")).willReturn(stmt);
        doReturn(Mono.just(result)).when(stmt).execute();

        InfrastructureReactiveHealthIndicator indicator =
                new InfrastructureReactiveHealthIndicator(redisTemplate, factory);

        StepVerifier.create(indicator.health())
                .expectNextMatches(health -> health.getStatus() == Status.UP)
                .expectComplete()
                .verify();
    }

    @Test
    void healthReturnsDownWhenRedisFails() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        given(redisTemplate.execute(any(org.springframework.data.redis.core.ReactiveRedisCallback.class)))
                .willReturn(Flux.error(new RuntimeException("redis down")));

        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        Result result = mock(Result.class);
        doReturn(Mono.<Connection>just(conn)).when(factory).create();
        given(conn.createStatement("SELECT 1")).willReturn(stmt);
        doReturn(Mono.just(result)).when(stmt).execute();

        InfrastructureReactiveHealthIndicator indicator =
                new InfrastructureReactiveHealthIndicator(redisTemplate, factory);

        StepVerifier.create(indicator.health())
                .expectNextMatches(health -> health.getStatus() == Status.DOWN)
                .expectComplete()
                .verify();
    }

    @Test
    void healthReturnsDownWhenPostgresFails() {
        ReactiveStringRedisTemplate redisTemplate = mock(ReactiveStringRedisTemplate.class);
        given(redisTemplate.execute(any(org.springframework.data.redis.core.ReactiveRedisCallback.class))).willReturn(Flux.just("PONG"));

        ConnectionFactory factory = mock(ConnectionFactory.class);
        doReturn(Mono.error(new RuntimeException("pg down"))).when(factory).create();

        InfrastructureReactiveHealthIndicator indicator =
                new InfrastructureReactiveHealthIndicator(redisTemplate, factory);

        StepVerifier.create(indicator.health())
                .expectNextMatches(health -> health.getStatus() == Status.DOWN)
                .expectComplete()
                .verify();
    }
}