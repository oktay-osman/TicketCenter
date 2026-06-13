package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Long> {
}