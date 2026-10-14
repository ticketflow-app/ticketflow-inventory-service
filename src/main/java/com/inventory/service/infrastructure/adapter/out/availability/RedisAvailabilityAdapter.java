package com.inventory.service.infrastructure.adapter.out.availability;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.domain.model.Availability;
import com.inventory.service.domain.model.LocalityAvailability;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RedisAvailabilityAdapter implements AvailabilityRepository {

    private static final String VIP = "VIP";
    private static final String GENERAL = "General";
    private static final String KEY_PREFIX = "inventory:availability:";

    private final StringRedisTemplate redisTemplate;

    public RedisAvailabilityAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Retry(name = "inventoryRetry")
    public Availability getAvailability(Long eventId) {
        LocalityAvailability vip = new LocalityAvailability(VIP, getAvailableTickets(eventId, VIP));
        LocalityAvailability general = new LocalityAvailability(GENERAL, getAvailableTickets(eventId, GENERAL));
        return new Availability(eventId, List.of(vip, general));
    }

    /**
     * {@code DECRBY}: Redis is single-threaded, so the decrement is atomic and concurrent
     * requests observe a consistent counter. When the result is negative the decrement is
     * restored ({@code INCRBY}) and the request is rejected, so no seat is oversold.
     *
     * <p>No {@code @Retry} here on purpose: retrying a partially applied decrement could
     * decrement twice. The compensation itself is retried by {@link #release}.</p>
     */
    @Override
    public boolean tryReserve(Long eventId, String locality, int quantity) {
        String key = buildKey(eventId, locality);
        Long remaining = redisTemplate.opsForValue().decrement(key, quantity);
        if (remaining != null && remaining < 0) {
            redisTemplate.opsForValue().increment(key, quantity);
            return false;
        }
        return true;
    }

    @Override
    @Retry(name = "inventoryRetry")
    public void release(Long eventId, String locality, int quantity) {
        redisTemplate.opsForValue().increment(buildKey(eventId, locality), quantity);
    }

    @Override
    public void setAvailability(Long eventId, String locality, int value) {
        redisTemplate.opsForValue().set(buildKey(eventId, locality), String.valueOf(value));
    }

    @Override
    public Integer getCounter(Long eventId, String locality) {
        String value = redisTemplate.opsForValue().get(buildKey(eventId, locality));
        return value == null ? null : Integer.valueOf(value);
    }

    private Integer getAvailableTickets(Long eventId, String locality) {
        String value = redisTemplate.opsForValue().get(buildKey(eventId, locality));
        return value == null ? 0 : Integer.parseInt(value);
    }

    private String buildKey(Long eventId, String locality) {
        return KEY_PREFIX + eventId + ":" + locality;
    }
}
