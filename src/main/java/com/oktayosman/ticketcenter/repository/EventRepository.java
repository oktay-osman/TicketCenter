package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.model.Distributor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizer(Organizer organizer);

    /**
     * Reads an event with a row-level write lock so that sales for the same event
     * serialize. Needed because the capacity limit is an event-wide total that
     * per-seat-type locks alone would not protect. Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

    @Query("select distinct e from Event e join e.distributorAssignments ed where ed.distributor = :distributor")
    List<Event> findEventsByDistributor(@Param("distributor") Distributor distributor);

    @Query("select distinct e from Event e left join fetch e.organizer o left join fetch o.user")
    List<Event> findAllWithOrganizerUser();
}
