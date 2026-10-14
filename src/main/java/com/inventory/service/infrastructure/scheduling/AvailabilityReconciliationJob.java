package com.inventory.service.infrastructure.scheduling;

import com.inventory.service.application.port.in.ReconcileAvailabilityUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic reconciliation of the Redis availability counters against PostgreSQL (DA-001).
 *
 * <p>The interval is configurable through {@code inventory.reconciliation.*} because the
 * context does not define a value for this prototype.</p>
 */
@Component
public class AvailabilityReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityReconciliationJob.class);

    private final ReconcileAvailabilityUseCase reconcileAvailabilityUseCase;

    public AvailabilityReconciliationJob(ReconcileAvailabilityUseCase reconcileAvailabilityUseCase) {
        this.reconcileAvailabilityUseCase = reconcileAvailabilityUseCase;
    }

    @Scheduled(
            initialDelayString = "${inventory.reconciliation.initial-delay-ms:60000}",
            fixedDelayString = "${inventory.reconciliation.fixed-delay-ms:300000}")
    public void reconcile() {
        try {
            reconcileAvailabilityUseCase.reconcile();
        } catch (RuntimeException exception) {
            log.error("Availability reconciliation failed", exception);
        }
    }
}
