package com.oktayosman.ticketcenter.model;

import jakarta.persistence.*;

@Entity
@PrimaryKeyJoinColumn(name = "user_id")
@Table(name = "distributors")
public class Distributor extends User {

    @Column(nullable = false)
    private double commissionRate;

    @Column
    private Float rating;

    public Distributor() {}

    public Distributor(String firstName, String lastName, String email, String username, String password, Role role, double commissionRate) {
        super(firstName, lastName, email, username, password, role);
        this.commissionRate = commissionRate;
        this.rating = null;
    }

    public double getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(double commissionRate) {
        this.commissionRate = commissionRate;
    }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }
}