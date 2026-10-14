package com.inventory.service.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA persistence model for the {@code reservations} table (audit log).
 *
 * <p>Kept in the infrastructure layer so the domain model stays free of JPA annotations.</p>
 */
@Entity
@Table(name = "reservations")
public class ReservationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "locality", nullable = false)
    private String locality;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "status", nullable = false)
    private String status;

    protected ReservationJpaEntity() {
    }

    public ReservationJpaEntity(
            Long id,
            Long eventId,
            String locality,
            Integer quantity,
            String status) {
        this.id = id;
        this.eventId = eventId;
        this.locality = locality;
        this.quantity = quantity;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getLocality() {
        return locality;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getStatus() {
        return status;
    }
}
