package com.inventory.service.infrastructure.adapter.out.kafka;

import com.inventory.service.application.domain.event.ReservationConfirmedEvent;
import com.inventory.service.application.port.out.EventPublisherPort;
import com.inventory.service.infrastructure.config.ReactiveKafkaConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Kafka adapter for publishing reservation confirmation events using
 * {@link ReactiveKafkaProducerTemplate}. Fully non-blocking.
 *
 * <p>Resilience strategy: if the broker is unavailable or the send times out,
 * the error is logged and swallowed. The reservation in PostgreSQL/Redis is NOT
 * rolled back — the client receives a successful response and the incident is
 * recorded for later reconciliation.
 */
@Component
public class KafkaEventPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisherAdapter.class);

    private final KafkaSender<String, ReservationConfirmedEvent> producerTemplate;
    private final String topic;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public KafkaEventPublisherAdapter(
            KafkaSender<String, ReservationConfirmedEvent> producerTemplate,
            ReactiveKafkaConfig config,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.producerTemplate = producerTemplate;
        this.topic = config.getReservationConfirmedTopic();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("kafkaAdapter");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("kafkaAdapter");
    }

    @Override
    public Mono<Void> publishReservationConfirmed(ReservationConfirmedEvent event) {
        return Mono.defer(() -> {
            SenderRecord<String, ReservationConfirmedEvent, String> record = SenderRecord.create(
                    new ProducerRecord<>(topic, event.reservationId(), event),
                    event.correlationId()
            );
            return producerTemplate.send(Mono.just(record))
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                    .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                    .doOnNext(result -> log.info(
                            "ReservationConfirmedEvent published [topic={}, partition={}, offset={}] " +
                                    "reservationId={}, eventId={}, localityId={}, quantity={}, correlationId={}",
                            result.recordMetadata().topic(),
                            result.recordMetadata().partition(),
                            result.recordMetadata().offset(),
                            event.reservationId(),
                            event.eventId(),
                            event.localityId(),
                            event.quantity(),
                            event.correlationId()))
                    .doOnError(throwable -> log.error(
                            "Failed to publish ReservationConfirmedEvent [reservationId={}, eventId={}, localityId={}, " +
                                    "quantity={}, correlationId={}]. Error: {}",
                            event.reservationId(),
                            event.eventId(),
                            event.localityId(),
                            event.quantity(),
                            event.correlationId(),
                            throwable.getMessage(),
                            throwable))
                    .onErrorResume(throwable -> Flux.empty())
                    .then();
        });
    }
}
