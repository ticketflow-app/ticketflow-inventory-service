package com.inventory.service.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link PostgreSQLHealthIndicator}.
 * Verifies UP when PostgreSQL responds to SELECT 1 and DOWN when it fails.
 */
class PostgreSQLHealthIndicatorTest {

    @Test
    void healthReturnsUpWhenPostgresResponds() {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection conn = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        Result result = mock(Result.class);

        doReturn(Mono.<Connection>just(conn)).when(factory).create();
        given(conn.createStatement("SELECT 1")).willReturn(stmt);
        doReturn(Mono.just(result)).when(stmt).execute();

        PostgreSQLHealthIndicator indicator = new PostgreSQLHealthIndicator(factory);

        StepVerifier.create(indicator.health())
                .expectNextMatches(health -> health.getStatus() == Status.UP)
                .expectComplete()
                .verify();
    }

    @Test
    void healthReturnsDownWhenPostgresFails() {
        ConnectionFactory factory = mock(ConnectionFactory.class);

        doReturn(Mono.error(new RuntimeException("connection refused"))).when(factory).create();

        PostgreSQLHealthIndicator indicator = new PostgreSQLHealthIndicator(factory);

        StepVerifier.create(indicator.health())
                .expectNextMatches(health -> health.getStatus() == Status.DOWN)
                .expectComplete()
                .verify();
    }
}