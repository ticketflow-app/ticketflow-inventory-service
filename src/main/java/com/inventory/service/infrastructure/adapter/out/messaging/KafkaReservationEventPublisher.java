package com.inventory.service.infrastructure.adapter.out.messaging;

import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.domain.model.Reservation;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class KafkaReservationEventPublisher implements ReservationEventPublisher {

    private static final String TOPIC = "reservation-created";

    private final KafkaTemplate<String, Reservation> kafkaTemplate;

    public KafkaReservationEventPublisher(
            KafkaTemplate<String, Reservation> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Mono<Void> publish(Reservation reservation) {
        return Mono.fromFuture(
                kafkaTemplate.send(
                        TOPIC,
                        reservation.getId().toString(),
                        reservation
                )
        ).then();
    }
}