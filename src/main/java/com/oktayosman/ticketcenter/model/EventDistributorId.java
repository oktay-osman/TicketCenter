package com.oktayosman.ticketcenter.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EventDistributorId implements Serializable {

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "distributor_id")
    private Long distributorId;

    public EventDistributorId() {
    }

    public EventDistributorId(Long eventId, Long distributorId) {
        this.eventId = eventId;
        this.distributorId = distributorId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getDistributorId() {
        return distributorId;
    }

    public void setDistributorId(Long distributorId) {
        this.distributorId = distributorId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventDistributorId that)) {
            return false;
        }
        return Objects.equals(eventId, that.eventId)
                && Objects.equals(distributorId, that.distributorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, distributorId);
    }
}
