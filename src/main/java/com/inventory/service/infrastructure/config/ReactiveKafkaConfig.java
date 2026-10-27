package com.inventory.service.infrastructure.config;

import com.inventory.service.application.domain.event.ReservationConfirmedEvent;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for reactive Kafka producer used to publish {@link ReservationConfirmedEvent}.
 * Uses {@link ReactiveKafkaProducerTemplate} for fully non-blocking, backpressure-aware sends.
 */
@Configuration
public class ReactiveKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.topic.reservation-confirmed:inventory.reservations.confirmed}")
    private String reservationConfirmedTopic;

    /**
     * Sender options for reservation-confirmed events.
     * acks=all, retries=3, delivery.timeout.ms=30s, request.timeout.ms=10s.
     */
    @Bean
    public SenderOptions<String, ReservationConfirmedEvent> reservationEventSenderOptions() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
        return SenderOptions.create(props);
    }

    @Bean
    public KafkaSender<String, ReservationConfirmedEvent> reservationEventKafkaSender(
            SenderOptions<String, ReservationConfirmedEvent> senderOptions) {
        return KafkaSender.create(senderOptions);
    }

    public String getReservationConfirmedTopic() {
        return reservationConfirmedTopic;
    }
}