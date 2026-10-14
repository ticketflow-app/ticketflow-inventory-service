package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.Reservation;

public interface ReservationEventPublisher {

    void publish(Reservation reservation);
}
