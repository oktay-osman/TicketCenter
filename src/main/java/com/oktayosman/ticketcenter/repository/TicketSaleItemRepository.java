package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.TicketSale;
import com.oktayosman.ticketcenter.model.TicketSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketSaleItemRepository extends JpaRepository<TicketSaleItem, Long> {
    List<TicketSaleItem> findByTicketSale(TicketSale ticketSale);
}
