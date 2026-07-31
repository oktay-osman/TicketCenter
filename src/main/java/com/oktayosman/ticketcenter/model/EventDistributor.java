package com.oktayosman.ticketcenter.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_distributor")
public class EventDistributor {

    @EmbeddedId
    private EventDistributorId id = new EventDistributorId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("distributorId")
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    public EventDistributor() {
    }

    public EventDistributor(Event event, Distributor distributor) {
        this.event = event;
        this.distributor = distributor;
    }

    public EventDistributorId getId() {
        return id;
    }

    public void setId(EventDistributorId id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Distributor getDistributor() {
        return distributor;
    }

    public void setDistributor(Distributor distributor) {
        this.distributor = distributor;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    @PrePersist
    protected void onAssign() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
