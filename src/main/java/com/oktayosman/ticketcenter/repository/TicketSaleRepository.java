package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.TicketSale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TicketSaleRepository extends JpaRepository<TicketSale, Long> {
    @Query("SELECT DISTINCT s FROM TicketSale s JOIN FETCH s.event LEFT JOIN FETCH s.items WHERE s.distributor = :distributor")
    List<TicketSale> findByDistributor(@Param("distributor") Distributor distributor);
    List<TicketSale> findByEvent(Event event);
    List<TicketSale> findByDistributorAndEvent(Distributor distributor, Event event);

    @Query("SELECT DISTINCT s FROM TicketSale s JOIN FETCH s.event LEFT JOIN FETCH s.items WHERE s.distributor = :distributor AND s.createdAt >= :from AND s.createdAt <= :to")
    List<TicketSale> findByDistributorAndDateRange(@Param("distributor") Distributor distributor,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM TicketSale s")
    BigDecimal getTotalRevenue();

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i")
    Long getTotalTicketsSold();

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i WHERE i.ticketSale.event = :event")
    Long getTicketsSoldForEvent(@Param("event") Event event);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM TicketSale s WHERE s.event = :event")
    BigDecimal getRevenueForEvent(@Param("event") Event event);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i WHERE i.ticketSale.distributor = :distributor")
    Long getTicketsSoldForDistributor(@Param("distributor") Distributor distributor);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM TicketSale s WHERE s.distributor = :distributor")
    BigDecimal getRevenueForDistributor(@Param("distributor") Distributor distributor);
}
