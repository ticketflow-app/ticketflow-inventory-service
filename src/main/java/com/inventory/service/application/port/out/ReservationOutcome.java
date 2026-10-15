package com.inventory.service.application.port.out;

/**
 * Outcome of an atomic reservation attempt over the availability of an event section.
 */
public enum ReservationOutcome {

    /** Availability was sufficient and was decremented. */
    RESERVED,

    /** The section is initialized but does not have enough availability; nothing was decremented. */
    INSUFFICIENT_AVAILABILITY,

    /** The section availability has not been initialized. */
    NOT_INITIALIZED
}
