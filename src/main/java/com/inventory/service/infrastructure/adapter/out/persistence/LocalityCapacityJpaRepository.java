package com.inventory.service.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocalityCapacityJpaRepository extends JpaRepository<LocalityCapacityJpaEntity, Long> {

    Optional<LocalityCapacityJpaEntity> findByEventIdAndLocality(Long eventId, String locality);
}
