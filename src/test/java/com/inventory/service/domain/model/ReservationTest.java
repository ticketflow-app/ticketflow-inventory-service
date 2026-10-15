package com.inventory.service.domain.model;

import com.inventory.service.domain.exception.InvalidReservationQuantityException;
import com.inventory.service.domain.exception.InvalidReservationStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private Reservation pendingReservation() {
        return new Reservation(
                1L,
                10L,
                "VIP",
                2,
                Reservation.STATUS_PENDING);
    }

    @Test
    void rejectsNullQuantity() {
        assertThatThrownBy(() -> new Reservation(
                1L, 10L, "VIP", null, Reservation.STATUS_PENDING))
                .isInstanceOf(InvalidReservationQuantityException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void rejectsZeroQuantity() {
        assertThatThrownBy(() -> new Reservation(
                1L, 10L, "VIP", 0, Reservation.STATUS_PENDING))
                .isInstanceOf(InvalidReservationQuantityException.class);
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> new Reservation(
                1L, 10L, "VIP", -3, Reservation.STATUS_PENDING))
                .isInstanceOf(InvalidReservationQuantityException.class);
    }

    @Test
    void confirmsFromPendingState() {
        Reservation pending = pendingReservation();

        Reservation confirmed = pending.confirm();

        assertThat(confirmed.getStatus()).isEqualTo(Reservation.STATUS_CONFIRMED);
        assertThat(confirmed.getId()).isEqualTo(1L);
        assertThat(confirmed.getEventId()).isEqualTo(10L);
        assertThat(confirmed.getLocality()).isEqualTo("VIP");
        assertThat(confirmed.getQuantity()).isEqualTo(2);
    }

    @Test
    void cancelsFromPendingState() {
        Reservation pending = pendingReservation();

        Reservation cancelled = pending.cancel();

        assertThat(cancelled.getStatus()).isEqualTo(Reservation.STATUS_CANCELLED);
        assertThat(cancelled.getId()).isEqualTo(1L);
        assertThat(cancelled.getQuantity()).isEqualTo(2);
    }

    @Test
    void confirmLeavesOriginalReservationUnchanged() {
        Reservation pending = pendingReservation();

        pending.confirm();

        assertThat(pending.getStatus()).isEqualTo(Reservation.STATUS_PENDING);
    }

    @Test
    void cancelLeavesOriginalReservationUnchanged() {
        Reservation pending = pendingReservation();

        pending.cancel();

        assertThat(pending.getStatus()).isEqualTo(Reservation.STATUS_PENDING);
    }

    @Test
    void rejectsConfirmFromConfirmedState() {
        Reservation confirmed = pendingReservation().confirm();

        assertThatThrownBy(confirmed::confirm)
                .isInstanceOf(InvalidReservationStateTransitionException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void rejectsConfirmFromCancelledState() {
        Reservation cancelled = pendingReservation().cancel();

        assertThatThrownBy(cancelled::confirm)
                .isInstanceOf(InvalidReservationStateTransitionException.class);
    }

    @Test
    void rejectsCancelFromConfirmedState() {
        Reservation confirmed = pendingReservation().confirm();

        assertThatThrownBy(confirmed::cancel)
                .isInstanceOf(InvalidReservationStateTransitionException.class);
    }

    @Test
    void rejectsCancelFromCancelledState() {
        Reservation cancelled = pendingReservation().cancel();

        assertThatThrownBy(cancelled::cancel)
                .isInstanceOf(InvalidReservationStateTransitionException.class);
    }
}
