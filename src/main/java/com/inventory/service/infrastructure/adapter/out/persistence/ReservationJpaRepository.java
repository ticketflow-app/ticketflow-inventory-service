package com.inventory.service.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    @Query("select coalesce(sum(r.quantity), 0) from ReservationJpaEntity r "
            + "where r.eventId = :eventId and r.locality = :locality and r.status = 'CONFIRMED'")
    long sumConfirmedQuantity(@Param("eventId") Long eventId, @Param("locality") String locality);
}
