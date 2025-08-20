package com.oktayosman.ticketcenter.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "ticket_limit")
    private int ticketLimit;

    @ManyToOne
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<Ticket> tickets;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Event() {}

    public Event(String name, String location, LocalDateTime eventDate, int ticketLimit, Organizer organizer) {
        this.name = name;
        this.location = location;
        this.eventDate = eventDate;
        this.ticketLimit = ticketLimit;
        this.organizer = organizer;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public int getTicketLimit() {
        return ticketLimit;
    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}