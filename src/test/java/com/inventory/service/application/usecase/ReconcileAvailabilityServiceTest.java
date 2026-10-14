package com.inventory.service.application.usecase;

import com.inventory.service.application.port.out.AvailabilityRepository;
import com.inventory.service.application.port.out.LocalityCapacityRepository;
import com.inventory.service.application.port.out.ReservationIncidentRepository;
import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.LocalityCapacity;
import com.inventory.service.domain.model.ReservationIncident;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconcileAvailabilityServiceTest {

    @Mock
    private LocalityCapacityRepository localityCapacityRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private ReservationIncidentRepository reservationIncidentRepository;

    @InjectMocks
    private ReconcileAvailabilityService service;

    @Test
    void reconcile_whenDrift_rebuildsRedisFromDurableStateAndRecordsIncident() {
        when(localityCapacityRepository.findAll())
                .thenReturn(List.of(new LocalityCapacity(1L, 1L, "VIP", 100)));
        when(reservationRepository.sumConfirmedQuantity(1L, "VIP")).thenReturn(30L);
        when(availabilityRepository.getCounter(1L, "VIP")).thenReturn(50);

        service.reconcile();

        verify(availabilityRepository).setAvailability(1L, "VIP", 70);

        ArgumentCaptor<ReservationIncident> captor = ArgumentCaptor.forClass(ReservationIncident.class);
        verify(reservationIncidentRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("RECONCILIATION_REPAIR");
        assertThat(captor.getValue().getQuantity()).isEqualTo(20);
    }

    @Test
    void reconcile_whenConsistent_doesNothing() {
        when(localityCapacityRepository.findAll())
                .thenReturn(List.of(new LocalityCapacity(1L, 1L, "VIP", 100)));
        when(reservationRepository.sumConfirmedQuantity(1L, "VIP")).thenReturn(30L);
        when(availabilityRepository.getCounter(1L, "VIP")).thenReturn(70);

        service.reconcile();

        verify(availabilityRepository, never()).setAvailability(any(), any(), anyInt());
        verify(reservationIncidentRepository, never()).save(any());
    }

    @Test
    void reconcile_whenCounterMissing_initializesRedis() {
        when(localityCapacityRepository.findAll())
                .thenReturn(List.of(new LocalityCapacity(1L, 1L, "VIP", 100)));
        when(reservationRepository.sumConfirmedQuantity(1L, "VIP")).thenReturn(0L);
        when(availabilityRepository.getCounter(1L, "VIP")).thenReturn(null);

        service.reconcile();

        verify(availabilityRepository).setAvailability(1L, "VIP", 100);
        verify(reservationIncidentRepository).save(any());
    }
}
