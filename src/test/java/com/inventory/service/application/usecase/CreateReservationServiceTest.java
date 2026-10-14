package com.inventory.service.application.usecase;

import com.inventory.service.application.exception.InsufficientAvailabilityException;
import com.inventory.service.application.exception.ReservationPersistenceException;
import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.ReservationEventPublisher;
import com.inventory.service.application.port.out.ReservationIncidentRepository;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.Reservation;
import com.inventory.service.domain.model.ReservationIncident;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReservationServiceTest {

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationEventPublisher reservationEventPublisher;

    @Mock
    private ReservationIncidentRepository reservationIncidentRepository;

    @InjectMocks
    private CreateReservationService service;

    private static Reservation request() {
        return new Reservation(null, 1L, "VIP", 2, "CONFIRMED");
    }

    @Test
    void createReservation_reservesPersistsAndPublishes() {
        Reservation request = request();
        Reservation saved = new Reservation(10L, 1L, "VIP", 2, "CONFIRMED");
        when(availabilityRepository.tryReserve(1L, "VIP", 2)).thenReturn(true);
        when(reservationRepository.save(request)).thenReturn(saved);

        Reservation result = service.createReservation(request);

        assertThat(result).isSameAs(saved);
        verify(reservationEventPublisher).publish(saved);
        verify(reservationIncidentRepository, never()).save(any());
    }

    @Test
    void createReservation_whenInsufficientAvailability_rejectsWithoutPersisting() {
        Reservation request = request();
        when(availabilityRepository.tryReserve(1L, "VIP", 2)).thenReturn(false);

        assertThatThrownBy(() -> service.createReservation(request))
                .isInstanceOf(InsufficientAvailabilityException.class);

        verify(reservationRepository, never()).save(any());
        verify(reservationEventPublisher, never()).publish(any());
        verify(reservationIncidentRepository, never()).save(any());
    }

    @Test
    void createReservation_whenPersistenceFails_compensatesAndRecordsIncident() {
        Reservation request = request();
        when(availabilityRepository.tryReserve(1L, "VIP", 2)).thenReturn(true);
        when(reservationRepository.save(request)).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.createReservation(request))
                .isInstanceOf(ReservationPersistenceException.class);

        verify(availabilityRepository).release(1L, "VIP", 2);

        ArgumentCaptor<ReservationIncident> captor = ArgumentCaptor.forClass(ReservationIncident.class);
        verify(reservationIncidentRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("PERSISTENCE_FAILURE_COMPENSATED");
        verify(reservationEventPublisher, never()).publish(any());
    }

    @Test
    void createReservation_whenCompensationFails_recordsCompensationFailure() {
        Reservation request = request();
        when(availabilityRepository.tryReserve(1L, "VIP", 2)).thenReturn(true);
        when(reservationRepository.save(request)).thenThrow(new RuntimeException("db down"));
        doThrow(new RuntimeException("redis down")).when(availabilityRepository).release(1L, "VIP", 2);

        assertThatThrownBy(() -> service.createReservation(request))
                .isInstanceOf(ReservationPersistenceException.class);

        ArgumentCaptor<ReservationIncident> captor = ArgumentCaptor.forClass(ReservationIncident.class);
        verify(reservationIncidentRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("COMPENSATION_FAILED");
    }

    @Test
    void createReservation_whenPublishFails_recordsIncidentAndStillReturnsReservation() {
        Reservation request = request();
        Reservation saved = new Reservation(10L, 1L, "VIP", 2, "CONFIRMED");
        when(availabilityRepository.tryReserve(1L, "VIP", 2)).thenReturn(true);
        when(reservationRepository.save(request)).thenReturn(saved);
        doThrow(new RuntimeException("kafka down")).when(reservationEventPublisher).publish(saved);

        Reservation result = service.createReservation(request);

        assertThat(result).isSameAs(saved);
        ArgumentCaptor<ReservationIncident> captor = ArgumentCaptor.forClass(ReservationIncident.class);
        verify(reservationIncidentRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("EVENT_PUBLISH_FAILURE");
    }

    @Test
    void createReservation_whenQuantityIsNotPositive_throwsIllegalArgument() {
        Reservation invalid = new Reservation(null, 1L, "VIP", 0, "CONFIRMED");

        assertThatThrownBy(() -> service.createReservation(invalid))
                .isInstanceOf(IllegalArgumentException.class);

        verify(availabilityRepository, never()).tryReserve(any(), any(), anyInt());
    }
}
