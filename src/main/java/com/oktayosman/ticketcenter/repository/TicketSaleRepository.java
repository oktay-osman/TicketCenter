package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.TicketSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketSaleRepository extends JpaRepository<TicketSale, Long> {
    @Query("SELECT DISTINCT s FROM TicketSale s JOIN FETCH s.event LEFT JOIN FETCH s.items WHERE s.distributor = :distributor")
    List<TicketSale> findByDistributor(@Param("distributor") Distributor distributor);
    List<TicketSale> findByEvent(Event event);
    List<TicketSale> findByDistributorAndEvent(Distributor distributor, Event event);
}
