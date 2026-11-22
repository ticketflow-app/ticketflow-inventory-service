package com.inventory.service.infrastructure.adapter.out.projection;

import com.inventory.service.application.port.out.EventLocalitiesPort;
import com.inventory.service.application.port.out.EventProjectionPort;
import com.inventory.service.infrastructure.adapter.out.availability.InventoryRedisKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis-backed local projection of the catalog event configuration.
 *
 * <p>Localities are stored as a hash {@code inv:localities:{eventId}} whose fields are locality
 * ids and whose values are the declared capacities. A sorted set keeps the cleanup deadline of
 * each event, derived from the event date and a configurable retention window.
 */
@Repository
public class RedisEventProjectionAdapter implements EventLocalitiesPort, EventProjectionPort {

    private final ReactiveStringRedisTemplate redis;
    private final long retentionDays;

    public RedisEventProjectionAdapter(ReactiveStringRedisTemplate redis,
                                       @Value("${inventory.event-retention-days:30}") long retentionDays) {
        this.redis = redis;
        this.retentionDays = retentionDays;
    }

    @Override
    public Flux<EventLocality> findByEvent(String eventId) {
        if (isBlank(eventId)) {
            return Flux.error(new IllegalArgumentException("eventId must be provided"));
        }
        return redis.opsForHash().entries(InventoryRedisKeys.localitiesKey(eventId))
                .map(entry -> new EventLocality(
                        entry.getKey().toString(), Integer.valueOf(entry.getValue().toString())));
    }

    @Override
    public Mono<Void> savePublished(String eventId, Instant eventDate, List<EventLocality> localities) {
        if (isBlank(eventId) || localities == null || localities.isEmpty()) {
            return Mono.error(new IllegalArgumentException("eventId and localities must be provided"));
        }

        Map<String, String> capacities = new LinkedHashMap<>();
        for (EventLocality locality : localities) {
            if (locality == null || isBlank(locality.localityId()) || locality.capacity() == null) {
                continue;
            }
            capacities.put(locality.localityId(), String.valueOf(locality.capacity()));
        }
        if (capacities.isEmpty()) {
            return Mono.error(new IllegalArgumentException("no valid localities in event projection"));
        }

        double expiresAt = eventDate.plus(Duration.ofDays(retentionDays)).getEpochSecond();
        Mono<Boolean> stored = redis.opsForHash().putAll(InventoryRedisKeys.localitiesKey(eventId), capacities);
        Mono<Boolean> scheduled = redis.opsForZSet()
                .add(InventoryRedisKeys.EVENT_EXPIRY_KEY, eventId, expiresAt);
        return stored.then(scheduled).then();
    }

    @Override
    public Mono<Void> delete(String eventId) {
        if (isBlank(eventId)) {
            return Mono.error(new IllegalArgumentException("eventId must be provided"));
        }
        return redis.opsForHash().keys(InventoryRedisKeys.localitiesKey(eventId))
                .map(Object::toString)
                .collectList()
                .flatMap(localityIds -> {
                    List<String> keys = new ArrayList<>();
                    keys.add(InventoryRedisKeys.localitiesKey(eventId));
                    for (String localityId : localityIds) {
                        keys.add(InventoryRedisKeys.stockKey(eventId, localityId));
                        keys.add(InventoryRedisKeys.capacityKey(eventId, localityId));
                    }
                    return redis.delete(keys.toArray(String[]::new))
                            .then(redis.opsForZSet().remove(InventoryRedisKeys.EVENT_EXPIRY_KEY, eventId))
                            .then();
                });
    }

    @Override
    public Flux<String> findExpiredEventIds(Instant now) {
        return redis.opsForZSet().rangeByScore(
                InventoryRedisKeys.EVENT_EXPIRY_KEY, Range.closed(0.0, (double) now.getEpochSecond()));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
