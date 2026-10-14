package com.inventory.service.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA persistence model for the {@code locality_capacity} table (HU-11).
 */
@Entity
@Table(name = "locality_capacity",
        uniqueConstraints = @UniqueConstraint(name = "uq_locality_capacity", columnNames = {"event_id", "locality"}))
public class LocalityCapacityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "locality", nullable = false)
    private String locality;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    protected LocalityCapacityJpaEntity() {
    }

    public LocalityCapacityJpaEntity(Long id, Long eventId, String locality, Integer capacity) {
        this.id = id;
        this.eventId = eventId;
        this.locality = locality;
        this.capacity = capacity;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
}
