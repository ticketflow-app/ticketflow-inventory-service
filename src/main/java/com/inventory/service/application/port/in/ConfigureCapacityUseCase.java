package com.inventory.service.application.port.in;

public interface ConfigureCapacityUseCase {

    /**
     * Registers the total capacity for a locality (HU-11) and initializes the
     * availability counter in Redis accordingly.
     */
    void configure(Long eventId, String locality, int capacity);
}
