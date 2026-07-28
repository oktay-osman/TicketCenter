package com.oktayosman.ticketcenter.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ticket_sales")
public class TicketSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "distributor_id", nullable = false)
    private Distributor distributor;

    @Column(name = "buyer_first_name", nullable = false)
    private String buyerFirstName;

    @Column(name = "buyer_last_name", nullable = false)
    private String buyerLastName;

    @Column(name = "buyer_email", nullable = false)
    private String buyerEmail;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "ticketSale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketSaleItem> items;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TicketSale() {}

    public TicketSale(Event event, Distributor distributor, String buyerFirstName,
                      String buyerLastName, String buyerEmail, BigDecimal totalAmount) {
        this.event = event;
        this.distributor = distributor;
        this.buyerFirstName = buyerFirstName;
        this.buyerLastName = buyerLastName;
        this.buyerEmail = buyerEmail;
        this.totalAmount = totalAmount;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
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

    public String getBuyerFirstName() {
        return buyerFirstName;
    }

    public void setBuyerFirstName(String buyerFirstName) {
        this.buyerFirstName = buyerFirstName;
    }

    public String getBuyerLastName() {
        return buyerLastName;
    }

    public void setBuyerLastName(String buyerLastName) {
        this.buyerLastName = buyerLastName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<TicketSaleItem> getItems() {
        return items;
    }

    public void setItems(List<TicketSaleItem> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
