package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.exception.TicketInventoryException;
import com.oktayosman.ticketcenter.exception.TicketLimitExceededException;
import com.oktayosman.ticketcenter.model.*;
import com.oktayosman.ticketcenter.repository.DistributorRatingRepository;
import com.oktayosman.ticketcenter.repository.DistributorRepository;
import com.oktayosman.ticketcenter.repository.EventDistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import com.oktayosman.ticketcenter.repository.SeatTypeRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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
    private final SeatTypeRepository seatTypeRepository;
    private final EventService eventService;
    private final NotificationService notificationService;

    public DistributorService(DistributorRepository distributorRepository,
                              DistributorRatingRepository distributorRatingRepository,
                              OrganizerRepository organizerRepository,
                              EventRepository eventRepository,
                              EventDistributorRepository eventDistributorRepository,
                              TicketSaleRepository ticketSaleRepository,
                              TicketSaleItemRepository ticketSaleItemRepository,
                              SeatTypeRepository seatTypeRepository,
                              EventService eventService,
                              NotificationService notificationService) {
        this.distributorRepository = distributorRepository;
        this.distributorRatingRepository = distributorRatingRepository;
        this.organizerRepository = organizerRepository;
        this.eventRepository = eventRepository;
        this.eventDistributorRepository = eventDistributorRepository;
        this.ticketSaleRepository = ticketSaleRepository;
        this.ticketSaleItemRepository = ticketSaleItemRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.eventService = eventService;
        this.notificationService = notificationService;
    }

    @Transactional
    public TicketSale createTicketSale(TicketSale ticketSale) {
        if (ticketSale.getItems() == null || ticketSale.getItems().isEmpty()) {
            throw new IllegalArgumentException("A ticket sale must contain at least one ticket.");
        }
        if (ticketSale.getEvent() == null || ticketSale.getEvent().getId() == null) {
            throw new IllegalArgumentException("A ticket sale must be linked to an event.");
        }

        // Re-read the event under a write lock: the caller passes objects detached from
        // an earlier load, so their seat counts may be stale, and the event-wide capacity
        // check below is an aggregate that the per-seat-type locks would not protect.
        // Locking the event first also fixes the lock order for every sale.
        Event event = eventRepository.findByIdForUpdate(ticketSale.getEvent().getId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found."));
        ticketSale.setEvent(event);

        // Collapse the requested quantities per seat type — the sale form allows several
        // rows pointing at the same seat type, and checking them one by one would let a
        // buyer split a request past the limits.
        Map<Long, Integer> requestedBySeatTypeId = new TreeMap<>();
        for (TicketSaleItem item : ticketSale.getItems()) {
            if (item.getSeatType() == null || item.getSeatType().getId() == null) {
                throw new IllegalArgumentException("Every ticket must have a ticket type.");
            }
            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
            if (quantity <= 0) {
                throw new IllegalArgumentException("Ticket quantities must be greater than 0.");
            }
            requestedBySeatTypeId.merge(item.getSeatType().getId(), quantity, Integer::sum);
        }

        int totalRequested = requestedBySeatTypeId.values().stream().mapToInt(Integer::intValue).sum();

        // Lock the seat types in ascending id order (TreeMap) so concurrent sales that
        // touch overlapping seat types cannot deadlock against each other.
        Map<Long, SeatType> lockedSeatTypes = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : requestedBySeatTypeId.entrySet()) {
            SeatType seatType = seatTypeRepository.findByIdForUpdate(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Ticket type no longer exists."));

            if (seatType.getEvent() == null || !event.getId().equals(seatType.getEvent().getId())) {
                throw new IllegalArgumentException("Ticket type does not belong to the selected event.");
            }

            int requested = entry.getValue();
            int available = seatType.getAvailableSeats();
            if (requested > available) {
                throw new TicketInventoryException(String.format(
                        "Only %d %s left for '%s' — you requested %d.",
                        Math.max(available, 0), seatType.getSeatCategory().getDisplayName(),
                        event.getName(), requested));
            }
            lockedSeatTypes.put(entry.getKey(), seatType);
        }

        // Per-person limit across every distributor selling this event.
        if (event.getTicketLimit() > 0) {
            String buyerEmail = ticketSale.getBuyerEmail() != null
                    ? ticketSale.getBuyerEmail().trim().toLowerCase()
                    : "";
            Long alreadyBought = ticketSaleRepository.getTicketsSoldForEventAndBuyer(event, buyerEmail);
            long buyerTotal = (alreadyBought != null ? alreadyBought : 0L) + totalRequested;
            if (buyerTotal > event.getTicketLimit()) {
                throw new TicketLimitExceededException(String.format(
                        "'%s' allows %d tickets per person. %s already has %d, so %d more cannot be sold.",
                        event.getName(), event.getTicketLimit(), ticketSale.getBuyerEmail(),
                        alreadyBought != null ? alreadyBought : 0L, totalRequested));
            }
        }

        // Event-wide capacity, on top of the per-seat-type totals.
        if (event.getCapacity() > 0) {
            long soldForEvent = getTicketsSoldByEvent(event);
            if (soldForEvent + totalRequested > event.getCapacity()) {
                throw new TicketInventoryException(String.format(
                        "'%s' has %d of %d tickets left — you requested %d.",
                        event.getName(), Math.max(event.getCapacity() - soldForEvent, 0),
                        event.getCapacity(), totalRequested));
            }
        }

        // Everything checks out: claim the inventory and price the sale from the
        // persisted seat types rather than the values the form sent.
        BigDecimal total = BigDecimal.ZERO;
        for (TicketSaleItem item : ticketSale.getItems()) {
            SeatType seatType = lockedSeatTypes.get(item.getSeatType().getId());
            BigDecimal unitPrice = seatType.getPrice();
            BigDecimal subtotal = unitPrice.multiply(new BigDecimal(item.getQuantity()));

            item.setSeatType(seatType);
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            item.setTicketSale(ticketSale);
            total = total.add(subtotal);
        }
        for (Map.Entry<Long, Integer> entry : requestedBySeatTypeId.entrySet()) {
            SeatType seatType = lockedSeatTypes.get(entry.getKey());
            seatType.setSoldSeats(seatType.getSoldSeats() + entry.getValue());
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

    @Transactional
    public void deleteTicketSale(Long id) {
        TicketSale ticketSale = ticketSaleRepository.findById(id).orElse(null);
        if (ticketSale == null) {
            return;
        }
        // Release the inventory the sale was holding, otherwise the seats stay
        // permanently unsellable.
        if (ticketSale.getItems() != null) {
            for (TicketSaleItem item : ticketSale.getItems()) {
                if (item.getSeatType() == null || item.getSeatType().getId() == null) {
                    continue;
                }
                SeatType seatType = seatTypeRepository.findByIdForUpdate(item.getSeatType().getId())
                        .orElse(null);
                if (seatType != null) {
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    seatType.setSoldSeats(Math.max(seatType.getSoldSeats() - quantity, 0));
                }
            }
        }
        ticketSaleRepository.delete(ticketSale);
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

    public long getTicketsSoldByEvent(Event event) {
        Long count = ticketSaleRepository.getTicketsSoldForEvent(event);
        return count != null ? count : 0L;
    }

    public BigDecimal getRevenueByEvent(Event event) {
        BigDecimal revenue = ticketSaleRepository.getRevenueForEvent(event);
        return revenue != null ? revenue : BigDecimal.ZERO;
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
