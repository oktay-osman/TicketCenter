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
}