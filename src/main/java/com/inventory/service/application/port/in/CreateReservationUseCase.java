package com.inventory.service.application.port.in;

import com.inventory.service.domain.model.Reservation;

public interface CreateReservationUseCase {

    Reservation createReservation(Reservation reservation);
}
