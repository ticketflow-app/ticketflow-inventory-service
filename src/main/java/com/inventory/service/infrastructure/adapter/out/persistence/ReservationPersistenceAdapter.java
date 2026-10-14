package com.inventory.service.infrastructure.adapter.out.persistence;

import com.inventory.service.application.port.out.ReservationRepository;
import com.inventory.service.domain.model.Reservation;
import org.springframework.stereotype.Repository;

@Repository
public class ReservationPersistenceAdapter implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;

    public ReservationPersistenceAdapter(ReservationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        ReservationJpaEntity entity = new ReservationJpaEntity(
                reservation.getId(),
                reservation.getEventId(),
                reservation.getLocality(),
                reservation.getQuantity(),
                reservation.getStatus()
        );

        ReservationJpaEntity saved = jpaRepository.save(entity);

        return new Reservation(saved.getId(), saved.getEventId(), saved.getLocality(), saved.getQuantity(), saved.getStatus());
    }

    @Override
    public long sumConfirmedQuantity(Long eventId, String locality) {
        return jpaRepository.sumConfirmedQuantity(eventId, locality);
    }
}
