package com.oktayosman.ticketcenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean sold;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    public Ticket() {}

    public Ticket(Event event, Distributor distributor, BigDecimal price, boolean sold) {
        this.event = event;
        this.distributor = distributor;
        this.price = price;
        this.sold = sold;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Distributor getDistributor() {
        return distributor;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isSold() {
        return sold;
    }

    public LocalDateTime getSoldAt() {
        return soldAt;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
        if (sold) {
            this.soldAt = LocalDateTime.now();
        }
    }
}