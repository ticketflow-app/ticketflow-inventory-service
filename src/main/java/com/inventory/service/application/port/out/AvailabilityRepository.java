package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Availability;

public interface AvailabilityRepository {

    Availability getAvailability(Long eventId);

    /**
     * Atomically reserves {@code quantity} tickets for a locality (Redis {@code DECRBY}).
     *
     * @return {@code true} when the tickets were reserved (counter stayed >= 0),
     *         {@code false} when there was not enough availability (counter is restored).
     */
    boolean tryReserve(Long eventId, String locality, int quantity);

    /**
     * Releases {@code quantity} tickets previously reserved for a locality
     * (Redis {@code INCRBY}). Used as compensation when persistence fails.
     */
    void release(Long eventId, String locality, int quantity);

    /**
     * Overwrites the availability counter for a locality. Used to initialize the
     * counter and by the reconciliation process.
     */
    void setAvailability(Long eventId, String locality, int value);

    /**
     * @return the current raw counter value, or {@code null} when the key does not exist.
     */
    Integer getCounter(Long eventId, String locality);
}
