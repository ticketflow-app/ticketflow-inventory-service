package com.inventory.service.infrastructure.adapter.in.messaging;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.EventLocalitiesPort.EventLocality;
import com.inventory.service.application.port.out.EventProjectionPort;
import com.inventory.service.infrastructure.adapter.out.availability.InventoryRedisKeys;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Feeds the local Redis projection of catalog events and seeds the availability counters.
 *
 * <p>Inventory no longer calls catalog-service at request time. Catalog pushes published and
 * cancelled events; this consumer keeps only the derived data inventory needs. Processing is
 * idempotent by envelope eventId. With record-level auto acknowledgement, an exception leaves
 * the offset uncommitted so Kafka can deliver the message again. Counters expose processing and
 * duplicate rates to Prometheus for the load-test report.
 */
@Component
public class CatalogEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CatalogEventConsumer.class);
    private static final Duration DEDUPE_TTL = Duration.ofDays(7);
    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(10);

    private final CatalogEventPayload payload;
    private final EventProjectionPort projectionPort;
    private final AvailabilityRepository availabilityRepository;
    private final ReactiveStringRedisTemplate redis;
    private final Counter publishedCounter;
    private final Counter duplicatePublishedCounter;
    private final Counter cancelledCounter;
    private final Counter duplicateCancelledCounter;

    public CatalogEventConsumer(CatalogEventPayload payload,
                                 EventProjectionPort projectionPort,
                                 AvailabilityRepository availabilityRepository,
                                 ReactiveStringRedisTemplate redis,
                                 MeterRegistry meterRegistry) {
        this.payload = payload;
        this.projectionPort = projectionPort;
        this.availabilityRepository = availabilityRepository;
        this.redis = redis;
        this.publishedCounter = meterRegistry.counter("catalog.events.published.processed");
        this.duplicatePublishedCounter = meterRegistry.counter("catalog.events.published.duplicates");
        this.cancelledCounter = meterRegistry.counter("catalog.events.cancelled.processed");
        this.duplicateCancelledCounter = meterRegistry.counter("catalog.events.cancelled.duplicates");
    }

    @KafkaListener(topics = "${kafka.topic.events-published:eventos.publicados}",
            groupId = "${spring.kafka.consumer.group-id:inventory-service}")
    public void onPublished(ConsumerRecord<String, String> record) {
        CatalogEventPayload.Published published = payload.readPublished(record.value());
        String messageId = payload.readMessageId(record.value());
        List<EventLocality> localities = published.localities();

        dedupe(messageId)
                .flatMap(isNew -> {
                    if (!isNew) {
                        duplicatePublishedCounter.increment();
                        log.info("Skipping duplicate catalog event {}", messageId);
                        return Mono.empty();
                    }
                    Mono<Void> seed = Mono.when(localities.stream()
                            .map(locality -> availabilityRepository.initialize(
                                    published.eventId(), locality.localityId(), locality.capacity()))
                            .toList());
                    return projectionPort
                            .savePublished(published.eventId(), published.fecha(), localities)
                            .then(seed)
                            .doOnSuccess(ignored -> publishedCounter.increment());
                })
                .doOnError(error -> log.error("Failed to process eventos.publicados {}", messageId, error))
                .block(PROCESSING_TIMEOUT);
    }

    @KafkaListener(topics = "${kafka.topic.events-cancelled:eventos.cancelados}",
            groupId = "${spring.kafka.consumer.group-id:inventory-service}")
    public void onCancelled(ConsumerRecord<String, String> record) {
        String eventId = payload.readCancelledEventId(record.value());
        String messageId = payload.readMessageId(record.value());

        dedupe(messageId)
                .flatMap(isNew -> {
                    if (!isNew) {
                        duplicateCancelledCounter.increment();
                        log.info("Skipping duplicate catalog event {}", messageId);
                        return Mono.empty();
                    }
                    return projectionPort.delete(eventId)
                            .doOnSuccess(ignored -> cancelledCounter.increment());
                })
                .doOnError(error -> log.error("Failed to process eventos.cancelados {}", messageId, error))
                .block(PROCESSING_TIMEOUT);
    }

    private Mono<Boolean> dedupe(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return Mono.just(true);
        }
        return redis.opsForValue()
                .setIfAbsent(InventoryRedisKeys.processedKey(messageId), "1", DEDUPE_TTL)
                .map(Boolean::booleanValue);
    }
}
