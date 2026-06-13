package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

}
