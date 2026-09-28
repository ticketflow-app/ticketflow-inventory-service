package com.inventory.service.infrastructure.adapter.out.availability;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.domain.model.Availability;
import com.inventory.service.domain.model.LocalityAvailability;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import io.github.resilience4j.retry.annotation.Retry;

import java.util.List;

@Repository
public class RedisAvailabilityAdapter implements AvailabilityRepository {
        private final ReactiveStringRedisTemplate redisTemplate;
        public RedisAvailabilityAdapter(ReactiveStringRedisTemplate redisTemplate) {
                this.redisTemplate = redisTemplate;
        }

        @Override
        @Retry(name = "inventoryRetry")
        public Mono<Availability> getAvailability(Long eventId) {

        String vipKey = "inventory:availability:" + eventId + ":VIP";
        String generalKey = "inventory:availability:" + eventId + ":General";

        Mono<LocalityAvailability> vip =
                redisTemplate.opsForValue()
                        .get(vipKey)
                        .map(value -> new LocalityAvailability(
                                "VIP",
                                Integer.valueOf(value)
                        ))
                        .defaultIfEmpty(new LocalityAvailability("VIP", 0));
        Mono<LocalityAvailability> general =
                redisTemplate.opsForValue()
                        .get(generalKey)
                        .map(value -> new LocalityAvailability(
                                "General",
                                Integer.valueOf(value)
                        ))
                        .defaultIfEmpty(new LocalityAvailability("General", 0));

        return Mono.zip(vip, general)
                .map(result -> new Availability(
                        eventId,
                        List.of(result.getT1(), result.getT2())
                ));
        }
}