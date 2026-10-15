package com.inventory.service.domain.model;

import com.inventory.service.domain.exception.InvalidAvailabilityException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalityAvailabilityTest {

    @Test
    void rejectsNullAvailableTickets() {
        assertThatThrownBy(() -> new LocalityAvailability("VIP", null))
                .isInstanceOf(InvalidAvailabilityException.class)
                .hasMessageContaining("null or negative");
    }

    @Test
    void rejectsNegativeAvailableTickets() {
        assertThatThrownBy(() -> new LocalityAvailability("VIP", -1))
                .isInstanceOf(InvalidAvailabilityException.class);
    }

    @Test
    void acceptsZeroAvailableTickets() {
        LocalityAvailability availability = new LocalityAvailability("VIP", 0);

        assertThat(availability.getLocality()).isEqualTo("VIP");
        assertThat(availability.getAvailableTickets()).isZero();
    }

    @Test
    void acceptsPositiveAvailableTickets() {
        LocalityAvailability availability = new LocalityAvailability("General", 42);

        assertThat(availability.getAvailableTickets()).isEqualTo(42);
    }
}
