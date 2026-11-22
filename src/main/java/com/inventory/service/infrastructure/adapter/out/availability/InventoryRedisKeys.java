package com.inventory.service.infrastructure.adapter.out.availability;

/**
 * Central definition of the Redis keys owned by inventory-service.
 *
 * <p>Keeping the prefixes in one place avoids drift between the counter adapter, the
 * Kafka-backed catalog projection and the post-event cleanup job.
 */
public final class InventoryRedisKeys {

    public static final String STOCK_PREFIX = "inv:stock:";
    public static final String LOCALITIES_PREFIX = "inv:localities:";
    public static final String EVENT_EXPIRY_KEY = "inv:events:expiry";
    public static final String PROCESSED_PREFIX = "inv:processed:";

    private InventoryRedisKeys() {
    }

    public static String stockKey(String eventId, String localityId) {
        return STOCK_PREFIX + eventId + ":" + localityId;
    }

    public static String capacityKey(String eventId, String localityId) {
        return stockKey(eventId, localityId) + ":capacity";
    }

    public static String localitiesKey(String eventId) {
        return LOCALITIES_PREFIX + eventId;
    }

    public static String processedKey(String messageId) {
        return PROCESSED_PREFIX + messageId;
    }
}
