package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.SeatType;
import com.oktayosman.ticketcenter.model.TicketSale;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
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

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i "
            + "WHERE i.ticketSale.event = :event AND i.ticketSale.createdAt > :from AND i.ticketSale.createdAt <= :to")
    Long getTicketsSoldForEventAndDateRange(@Param("event") Event event,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    @Query("SELECT DISTINCT s.event FROM TicketSale s WHERE s.createdAt > :from AND s.createdAt <= :to")
    List<Event> findEventsWithSalesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i "
            + "WHERE i.ticketSale.event = :event AND LOWER(i.ticketSale.buyerEmail) = :email")
    Long getTicketsSoldForEventAndBuyer(@Param("event") Event event, @Param("email") String email);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i WHERE i.seatType = :seatType")
    Long getSoldQuantityForSeatType(@Param("seatType") SeatType seatType);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM TicketSale s WHERE s.event = :event")
    BigDecimal getRevenueForEvent(@Param("event") Event event);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i WHERE i.ticketSale.distributor = :distributor")
    Long getTicketsSoldForDistributor(@Param("distributor") Distributor distributor);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM TicketSale s WHERE s.distributor = :distributor")
    BigDecimal getRevenueForDistributor(@Param("distributor") Distributor distributor);

    // Batched (grouped) variants used by report screens so they don't run two
    // queries per row (see DistributorService.getSalesTotalsByEvent / getSalesTotalsByDistributors).
    @Query("SELECT i.ticketSale.event.id, COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i "
            + "WHERE i.ticketSale.event IN :events GROUP BY i.ticketSale.event.id")
    List<Object[]> getTicketsSoldGroupedByEvent(@Param("events") Collection<Event> events);

    @Query("SELECT s.event.id, COALESCE(SUM(s.totalAmount), 0) FROM TicketSale s "
            + "WHERE s.event IN :events GROUP BY s.event.id")
    List<Object[]> getRevenueGroupedByEvent(@Param("events") Collection<Event> events);

    @Query("SELECT i.ticketSale.distributor.id, COALESCE(SUM(i.quantity), 0) FROM TicketSaleItem i "
            + "WHERE i.ticketSale.distributor IN :distributors AND i.ticketSale.createdAt >= :from AND i.ticketSale.createdAt <= :to "
            + "GROUP BY i.ticketSale.distributor.id")
    List<Object[]> getTicketsSoldGroupedByDistributorInRange(@Param("distributors") Collection<Distributor> distributors,
                                                             @Param("from") LocalDateTime from,
                                                             @Param("to") LocalDateTime to);

    @Query("SELECT s.distributor.id, COALESCE(SUM(s.totalAmount), 0) FROM TicketSale s "
            + "WHERE s.distributor IN :distributors AND s.createdAt >= :from AND s.createdAt <= :to "
            + "GROUP BY s.distributor.id")
    List<Object[]> getRevenueGroupedByDistributorInRange(@Param("distributors") Collection<Distributor> distributors,
                                                          @Param("from") LocalDateTime from,
                                                          @Param("to") LocalDateTime to);
}
