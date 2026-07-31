package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.*;
import com.oktayosman.ticketcenter.repository.DistributorRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class DistributorService {

    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";

    private final DistributorRepository distributorRepository;
    private final TicketSaleRepository ticketSaleRepository;
    private final TicketSaleItemRepository ticketSaleItemRepository;
    private final EventService eventService;

    public DistributorService(DistributorRepository distributorRepository,
                              TicketSaleRepository ticketSaleRepository,
                              TicketSaleItemRepository ticketSaleItemRepository,
                              EventService eventService) {
        this.distributorRepository = distributorRepository;
        this.ticketSaleRepository = ticketSaleRepository;
        this.ticketSaleItemRepository = ticketSaleItemRepository;
        this.eventService = eventService;
    }

    public TicketSale createTicketSale(TicketSale ticketSale) {
        BigDecimal total = BigDecimal.ZERO;
        if (ticketSale.getItems() != null) {
            for (TicketSaleItem item : ticketSale.getItems()) {
                total = total.add(item.getSubtotal());
            }
        }
        ticketSale.setTotalAmount(total);
        return ticketSaleRepository.save(ticketSale);
    }

    public List<TicketSale> getDistributorSales(Distributor distributor) {
        return ticketSaleRepository.findByDistributor(distributor);
    }

    public List<TicketSale> getDistributorSalesByEvent(Distributor distributor, Event event) {
        return ticketSaleRepository.findByDistributorAndEvent(distributor, event);
    }

    public List<TicketSale> getSalesByEvent(Event event) {
        return ticketSaleRepository.findByEvent(event);
    }

    public Optional<Distributor> getDistributorByUserId(Long userId) {
        return distributorRepository.findByUser_IdAndUser_Role_Name(userId, DISTRIBUTOR_ROLE_NAME);
    }

    public Optional<TicketSale> getTicketSaleById(Long id) {
        return ticketSaleRepository.findById(id);
    }

    public void deleteTicketSale(Long id) {
        ticketSaleRepository.deleteById(id);
    }

    public BigDecimal calculateSaleTotal(List<TicketSaleItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(TicketSaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public TicketSaleItem addItemToSale(TicketSale ticketSale, SeatType seatType, Integer quantity) {
        BigDecimal unitPrice = seatType.getPrice();
        BigDecimal subtotal = unitPrice.multiply(new BigDecimal(quantity));
        
        TicketSaleItem item = new TicketSaleItem(ticketSale, seatType, quantity, unitPrice, subtotal);
        return ticketSaleItemRepository.save(item);
    }

    public List<Distributor> getAllDistributors() {
        return distributorRepository.findAll();
    }

    public List<Event> getEventsForDistributor(Distributor distributor) {
        return eventService.getEventsByDistributor(distributor);
    }
}
