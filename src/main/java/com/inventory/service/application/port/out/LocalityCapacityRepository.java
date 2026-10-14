package com.inventory.service.application.port.out;

import com.inventory.service.domain.model.LocalityCapacity;

import java.util.List;
import java.util.Optional;

public interface LocalityCapacityRepository {

    LocalityCapacity save(LocalityCapacity capacity);

    Optional<LocalityCapacity> find(Long eventId, String locality);

    List<LocalityCapacity> findAll();
}
