package com.inventory.service.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA persistence model for the {@code reservation_incidents} table (CU-06 FA-03, DA-001).
 */
@Entity
@Table(name = "reservation_incidents")
public class ReservationIncidentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "locality")
    private String locality;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "detail")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReservationIncidentJpaEntity() {
    }

    public ReservationIncidentJpaEntity(
            Long id,
            Long eventId,
            String locality,
            Integer quantity,
            String reason,
            String detail,
            Instant createdAt) {
        this.id = id;
        this.eventId = eventId;
        this.locality = locality;
        this.quantity = quantity;
        this.reason = reason;
        this.detail = detail;
        this.createdAt = createdAt;
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

    public String getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
