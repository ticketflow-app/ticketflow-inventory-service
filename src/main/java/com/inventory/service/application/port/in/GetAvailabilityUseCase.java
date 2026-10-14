package com.inventory.service.application.port.in;

import com.inventory.service.domain.model.Availability;

public interface GetAvailabilityUseCase {

    Availability getAvailability(Long eventId);
}
