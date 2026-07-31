package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.model.Distributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizer(Organizer organizer);

    @Query("select distinct e from Event e join e.distributorAssignments ed where ed.distributor = :distributor")
    List<Event> findEventsByDistributor(@Param("distributor") Distributor distributor);

    @Query("select distinct e from Event e left join fetch e.organizer o left join fetch o.user")
    List<Event> findAllWithOrganizerUser();
}
