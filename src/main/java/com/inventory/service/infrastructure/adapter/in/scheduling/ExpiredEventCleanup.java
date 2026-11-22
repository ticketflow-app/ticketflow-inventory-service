package com.inventory.service.infrastructure.adapter.in.scheduling;

import com.inventory.service.application.port.out.EventProjectionPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Deletes the Redis projection and counters of events whose retention deadline has passed.
 * The deadline is written when {@code eventos.publicados} is consumed, so no catalog query is
 * needed at cleanup time. Counters expose cleanup activity to Prometheus.
 */
@Component
public class ExpiredEventCleanup {

    private static final Logger log = LoggerFactory.getLogger(ExpiredEventCleanup.class);
    private static final Duration CLEANUP_TIMEOUT = Duration.ofSeconds(30);

    private final EventProjectionPort projectionPort;
    private final Counter runsCounter;
    private final Counter removedCounter;

    public ExpiredEventCleanup(EventProjectionPort projectionPort, MeterRegistry meterRegistry) {
        this.projectionPort = projectionPort;
        this.runsCounter = meterRegistry.counter("catalog.events.cleanup.runs");
        this.removedCounter = meterRegistry.counter("catalog.events.cleanup.events.removed");
    }

    @Scheduled(fixedDelayString = "${inventory.cleanup.interval-ms:3600000}",
            initialDelayString = "${inventory.cleanup.initial-delay-ms:60000}")
    public void cleanup() {
        runsCounter.increment();
        projectionPort.findExpiredEventIds(Instant.now())
                .concatMap(eventId -> projectionPort.delete(eventId)
                        .doOnSuccess(ignored -> {
                            removedCounter.increment();
                            log.info("Cleaned expired event projection {}", eventId);
                        })
                        .onErrorResume(error -> {
                            log.warn("Could not clean expired event {}", eventId, error);
                            return Mono.empty();
                        }))
                .then()
                .doOnError(error -> log.error("Expired event cleanup failed", error))
                .onErrorResume(error -> Mono.empty())
                .block(CLEANUP_TIMEOUT);
    }
}
