package com.inventory.service.application.port.out;

/**
 * Outcome of seeding the availability of an event section.
 */
public enum InitializeOutcome {

    /** The section availability and its capacity key were created. */
    INITIALIZED,

    /** The section was already initialized; the existing availability was left untouched. */
    ALREADY_INITIALIZED
}
