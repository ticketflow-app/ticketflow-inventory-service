package com.inventory.service.application.port.in;

public interface ReconcileAvailabilityUseCase {

    /**
     * Recomputes the Redis availability from the durable sources of truth
     * (capacity configured per locality minus confirmed reservations in PostgreSQL)
     * and repairs any drift, recording the corresponding incident (DA-001).
     */
    void reconcile();
}
