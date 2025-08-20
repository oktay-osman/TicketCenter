package com.oktayosman.ticketcenter.model;

import jakarta.persistence.*;

@Entity
@PrimaryKeyJoinColumn(name = "user_id")
@Table(name = "organizers")
public class Organizer extends User {

    @Column(nullable = false)
    private String organizationName;

    public Organizer() {}

    public Organizer(String firstName, String lastName, String email, String username, String password, Role role, String organizationName) {
        super(firstName, lastName, email, username, password, role);
        this.organizationName = organizationName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }
}