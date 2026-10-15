package com.inventory.service.infrastructure.adapter.out.availability;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.InitializeOutcome;
import com.inventory.service.application.port.out.ReleaseOutcome;
import com.inventory.service.application.port.out.ReservationOutcome;
import com.inventory.service.domain.model.Availability;
import com.inventory.service.domain.model.LocalityAvailability;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Reactive Redis adapter that manages ticket availability with atomic Lua operations,
 * preventing overselling and negative availability under concurrency (RB-08/RB-11).
 */
@Repository
public class RedisAvailabilityAdapter implements AvailabilityRepository {

    private static final String KEY_PREFIX = "availability:";
    private static final String CAPACITY_SUFFIX = ":capacity";

    /**
     * Native Reactor retry for reads (DA-002): 1 initial attempt + 2 retries with backoff,
     * equivalent to the former resilience4j inventoryRetry policy (max-attempts=3, 500ms).
     */
    private static final Retry READ_RETRY = Retry.backoff(2, Duration.ofMillis(500))
            .filter(ex -> !(ex instanceof IllegalArgumentException))
            .onRetryExhaustedThrow((spec, retrySignal) -> retrySignal.failure());

    private static final String INITIALIZE_SCRIPT =
            "if redis.call('EXISTS', KEYS[1]) == 1 then return \"0\" end "
          + "redis.call('SET', KEYS[2], ARGV[1]) "
          + "redis.call('SET', KEYS[1], ARGV[1]) "
          + "return \"1\"";

    private static final String TRY_RESERVE_SCRIPT =
            "local c = redis.call('GET', KEYS[1]) "
          + "if c == false then return \"-1\" end "
          + "if tonumber(c) < tonumber(ARGV[1]) then return \"0\" end "
          + "redis.call('DECRBY', KEYS[1], ARGV[1]) "
          + "return \"1\"";

    private static final String RELEASE_SCRIPT =
            "local cap = redis.call('GET', KEYS[2]) "
          + "if cap == false then return \"0\" end "
          + "local cur = tonumber(redis.call('GET', KEYS[1]) or \"0\") + tonumber(ARGV[1]) "
          + "if cur > tonumber(cap) then cur = tonumber(cap) end "
          + "redis.call('SET', KEYS[1], cur) "
          + "return \"1\"";

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisAvailabilityAdapter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<InitializeOutcome> initialize(Long eventId, String sectionId, Integer capacity) {
        if (eventId == null || sectionId == null || sectionId.isBlank()) {
            return Mono.error(new IllegalArgumentException("eventId and sectionId must be provided"));
        }
        if (capacity == null || capacity < 0) {
            return Mono.error(new IllegalArgumentException("capacity must be >= 0"));
        }
        return runScript(INITIALIZE_SCRIPT,
                        List.of(availabilityKey(eventId, sectionId), capacityKey(eventId, sectionId)),
                        capacity)
                .map(r -> "1".equals(r) ? InitializeOutcome.INITIALIZED : InitializeOutcome.ALREADY_INITIALIZED);
    }

    @Override
    public Mono<ReservationOutcome> tryReserve(Long eventId, String sectionId, Integer quantity) {
        if (eventId == null || sectionId == null || sectionId.isBlank()) {
            return Mono.error(new IllegalArgumentException("eventId and sectionId must be provided"));
        }
        if (quantity == null || quantity <= 0) {
            return Mono.error(new IllegalArgumentException("quantity must be > 0"));
        }
        return runScript(TRY_RESERVE_SCRIPT, List.of(availabilityKey(eventId, sectionId)), quantity)
                .map(this::toReservationOutcome);
    }

    @Override
    public Mono<ReleaseOutcome> release(Long eventId, String sectionId, Integer quantity) {
        if (eventId == null || sectionId == null || sectionId.isBlank()) {
            return Mono.error(new IllegalArgumentException("eventId and sectionId must be provided"));
        }
        if (quantity == null || quantity <= 0) {
            return Mono.error(new IllegalArgumentException("quantity must be > 0"));
        }
        return runScript(RELEASE_SCRIPT,
                        List.of(availabilityKey(eventId, sectionId), capacityKey(eventId, sectionId)),
                        quantity)
                .map(r -> "1".equals(r) ? ReleaseOutcome.RELEASED : ReleaseOutcome.NOT_INITIALIZED);
    }

    @Override
    public Mono<Availability> getAvailability(Long eventId) {
        Mono<LocalityAvailability> vip = readSection(eventId, "VIP");
        Mono<LocalityAvailability> general = readSection(eventId, "General");
        return Mono.zip(vip, general)
                .map(r -> new Availability(eventId, List.of(r.getT1(), r.getT2())))
                .retryWhen(READ_RETRY);
    }

    private Mono<LocalityAvailability> readSection(Long eventId, String section) {
        return redisTemplate.opsForValue()
                .get(availabilityKey(eventId, section))
                .map(v -> new LocalityAvailability(section, Integer.valueOf(v)))
                .defaultIfEmpty(new LocalityAvailability(section, 0));
    }

    private Mono<String> runScript(String lua, List<String> keys, Integer numberArg) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(lua);
        script.setResultType(String.class);
        return redisTemplate.execute(script, keys, String.valueOf(numberArg)).next();
    }

    private ReservationOutcome toReservationOutcome(String result) {
        return switch (result) {
            case "1" -> ReservationOutcome.RESERVED;
            case "0" -> ReservationOutcome.INSUFFICIENT_AVAILABILITY;
            default -> ReservationOutcome.NOT_INITIALIZED;
        };
    }

    private String availabilityKey(Long eventId, String sectionId) {
        return KEY_PREFIX + eventId + ":" + sectionId;
    }

    private String capacityKey(Long eventId, String sectionId) {
        return KEY_PREFIX + eventId + ":" + sectionId + CAPACITY_SUFFIX;
    }
}
