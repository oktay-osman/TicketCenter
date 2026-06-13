
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

    @Column(name = "ticket_limit_per_person")
    private int ticketLimit;

    // New fields
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50, nullable = false)
    private EventCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private EventStatus status;

    // maps to generic ticket limit column in DB
    @Column(name = "ticket_limit")
    private int capacity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imagePath;

    @Column(name = "organizer_id", nullable = false)
    private Integer organizerLegacyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_user_id", nullable = false)
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

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setTicketLimit(int ticketLimit) {
        this.ticketLimit = ticketLimit;
    }

    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    public Integer getOrganizerLegacyId() {
        return organizerLegacyId;
    }

    public void setOrganizerLegacyId(Integer organizerLegacyId) {
        this.organizerLegacyId = organizerLegacyId;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
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