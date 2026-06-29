package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizer(Organizer organizer);
}
