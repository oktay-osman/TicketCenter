package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.*;
import com.oktayosman.ticketcenter.repository.DistributorRatingRepository;
import com.oktayosman.ticketcenter.repository.DistributorRepository;
import com.oktayosman.ticketcenter.repository.EventDistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class DistributorService {

    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";

    private final DistributorRepository distributorRepository;
    private final DistributorRatingRepository distributorRatingRepository;
    private final OrganizerRepository organizerRepository;
    private final EventRepository eventRepository;
    private final EventDistributorRepository eventDistributorRepository;
    private final TicketSaleRepository ticketSaleRepository;
    private final TicketSaleItemRepository ticketSaleItemRepository;
    private final EventService eventService;
    private final NotificationService notificationService;

    public DistributorService(DistributorRepository distributorRepository,
                              DistributorRatingRepository distributorRatingRepository,
                              OrganizerRepository organizerRepository,
                              EventRepository eventRepository,
                              EventDistributorRepository eventDistributorRepository,
                              TicketSaleRepository ticketSaleRepository,
                              TicketSaleItemRepository ticketSaleItemRepository,
                              EventService eventService,
                              NotificationService notificationService) {
        this.distributorRepository = distributorRepository;
        this.distributorRatingRepository = distributorRatingRepository;
        this.organizerRepository = organizerRepository;
        this.eventRepository = eventRepository;
        this.eventDistributorRepository = eventDistributorRepository;
        this.ticketSaleRepository = ticketSaleRepository;
        this.ticketSaleItemRepository = ticketSaleItemRepository;
        this.eventService = eventService;
        this.notificationService = notificationService;
    }

    public TicketSale createTicketSale(TicketSale ticketSale) {
        BigDecimal total = BigDecimal.ZERO;
        if (ticketSale.getItems() != null) {
            for (TicketSaleItem item : ticketSale.getItems()) {
                total = total.add(item.getSubtotal());
            }
        }
        ticketSale.setTotalAmount(total);
        TicketSale saved = ticketSaleRepository.save(ticketSale);
        notificationService.notifyOrganizerTicketsSold(saved);
        return saved;
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

    @Transactional
    public Distributor rateDistributor(Long organizerUserId,
                                       Long eventId,
                                       Long distributorId,
                                       int ratingValue,
                                       String comment) {
        if (ratingValue < 1 || ratingValue > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        Organizer organizer = organizerRepository.findById(organizerUserId)
                .orElseThrow(() -> new IllegalStateException("Organizer profile not found."));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));
        Distributor distributor = distributorRepository.findById(distributorId)
                .orElseThrow(() -> new IllegalArgumentException("Distributor not found."));

        if (event.getOrganizer() == null || event.getOrganizer().getId() == null
                || !event.getOrganizer().getId().equals(organizer.getId())) {
            throw new IllegalStateException("You can only rate distributors for your own events.");
        }

        if (!eventDistributorRepository.existsByEventAndDistributor(event, distributor)) {
            throw new IllegalStateException("This distributor is not assigned to the selected event.");
        }

        DistributorRating distributorRating = distributorRatingRepository
                .findByOrganizerAndDistributorAndEvent(organizer, distributor, event)
                .orElseGet(DistributorRating::new);

        distributorRating.setOrganizer(organizer);
        distributorRating.setDistributor(distributor);
        distributorRating.setEvent(event);
        distributorRating.setRatingValue(ratingValue);
        distributorRating.setComment(comment != null && !comment.isBlank() ? comment.trim() : null);
        distributorRatingRepository.save(distributorRating);

        Double averageRating = distributorRatingRepository.findAverageByDistributor(distributor);
        distributor.setRating(averageRating != null ? averageRating.floatValue() : null);
        return distributorRepository.save(distributor);
    }
}
