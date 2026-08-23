package com.oktayosman.ticketcenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "seat_types")
public class SeatType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_category", nullable = false)
    private SeatCategory seatCategory;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "total_seats")
    private Integer totalSeats;

    @Column(name = "sold_seats", nullable = false, columnDefinition = "integer not null default 0")
    private int soldSeats;

    public SeatType() {}

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public SeatCategory getSeatCategory() {
        return seatCategory;
    }

    public void setSeatCategory(SeatCategory seatCategory) {
        this.seatCategory = seatCategory;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getSoldSeats() {
        return soldSeats;
    }

    public void setSoldSeats(int soldSeats) {
        this.soldSeats = soldSeats;
    }

    /**
     * Seats still on sale. A null totalSeats means the seat type is uncapped,
     * which is how legacy rows created before inventory tracking behave.
     */
    public int getAvailableSeats() {
        return totalSeats == null ? Integer.MAX_VALUE : totalSeats - soldSeats;
    }
}