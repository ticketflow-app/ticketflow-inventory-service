package com.inventory.service.application.port.out;

/**
 * Outcome of releasing availability back to an event section.
 */
public enum ReleaseOutcome {

    /** Availability was increased, capped at the initial capacity of the section. */
    RELEASED,

    /** The section availability has not been initialized. */
    NOT_INITIALIZED
}
