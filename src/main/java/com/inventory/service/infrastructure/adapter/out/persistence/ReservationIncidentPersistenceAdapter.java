package com.inventory.service.infrastructure.adapter.out.persistence;

import com.inventory.service.application.port.out.ReservationIncidentRepository;
import com.inventory.service.domain.model.ReservationIncident;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class ReservationIncidentPersistenceAdapter implements ReservationIncidentRepository {

    private final ReservationIncidentJpaRepository jpaRepository;

    public ReservationIncidentPersistenceAdapter(ReservationIncidentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ReservationIncident save(ReservationIncident incident) {
        ReservationIncidentJpaEntity entity = new ReservationIncidentJpaEntity(
                incident.getId(),
                incident.getEventId(),
                incident.getLocality(),
                incident.getQuantity(),
                incident.getReason(),
                incident.getDetail(),
                incident.getCreatedAt() == null ? Instant.now() : incident.getCreatedAt());

        ReservationIncidentJpaEntity saved = jpaRepository.save(entity);

        return new ReservationIncident(
                saved.getId(), saved.getEventId(), saved.getLocality(), saved.getQuantity(),
                saved.getReason(), saved.getDetail(), saved.getCreatedAt());
    }
}
