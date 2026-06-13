package com.oktayosman.ticketcenter.model;

import jakarta.persistence.*;

@Entity
@Table(name = "organizers")
public class Organizer {

    @Id
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String organizationName;

    private Double commission;

    public Organizer() {
    }

    public Organizer(User user, String organizationName, Double commission) {
        this.user = user;
        this.userId = user.getId();
        this.organizationName = organizationName;
        this.commission = commission;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.userId = user.getId();
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
    }
}