package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.ReservationIncident;

public interface ReservationIncidentRepository {

    ReservationIncident save(ReservationIncident incident);
}
