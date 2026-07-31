package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.EventDistributor;
import com.oktayosman.ticketcenter.model.EventDistributorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventDistributorRepository extends JpaRepository<EventDistributor, EventDistributorId> {
    void deleteByEvent(Event event);

    List<EventDistributor> findByEvent(Event event);

    List<EventDistributor> findByDistributor(Distributor distributor);

    boolean existsByEventAndDistributor(Event event, Distributor distributor);

    @Query("select ed.distributor from EventDistributor ed where ed.event.id = :eventId")
    List<Distributor> findDistributorsByEventId(@Param("eventId") Long eventId);
}
