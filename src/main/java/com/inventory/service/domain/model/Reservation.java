package com.inventory.service.domain.model;

public class Reservation {

    private Long id;
    private Long eventId;
    private String locality;
    private Integer quantity;
    private String status;

    public Reservation(
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