package com.inventory.service.infrastructure.adapter.out.persistence;

import com.inventory.service.application.port.out.LocalityCapacityRepository;
import com.inventory.service.domain.model.LocalityCapacity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LocalityCapacityPersistenceAdapter implements LocalityCapacityRepository {

    private final LocalityCapacityJpaRepository jpaRepository;

    public LocalityCapacityPersistenceAdapter(LocalityCapacityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LocalityCapacity save(LocalityCapacity capacity) {
        LocalityCapacityJpaEntity entity = jpaRepository
                .findByEventIdAndLocality(capacity.getEventId(), capacity.getLocality())
                .orElseGet(() -> new LocalityCapacityJpaEntity(
                        null, capacity.getEventId(), capacity.getLocality(), capacity.getCapacity()));

        entity.setCapacity(capacity.getCapacity());

        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<LocalityCapacity> find(Long eventId, String locality) {
        return jpaRepository.findByEventIdAndLocality(eventId, locality).map(this::toDomain);
    }

    @Override
    public List<LocalityCapacity> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private LocalityCapacity toDomain(LocalityCapacityJpaEntity entity) {
        return new LocalityCapacity(
                entity.getId(), entity.getEventId(), entity.getLocality(), entity.getCapacity());
    }
}
