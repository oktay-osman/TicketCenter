package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.EventDistributor;
import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.repository.EventDistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oktayosman.ticketcenter.model.SeatCategory;
import com.oktayosman.ticketcenter.model.SeatType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventDistributorRepository eventDistributorRepository;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepository,
                        EventDistributorRepository eventDistributorRepository,
                        NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.eventDistributorRepository = eventDistributorRepository;
        this.notificationService = notificationService;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<Event> getAllEventsWithOrganizerUser() {
        return eventRepository.findAllWithOrganizerUser();
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    @Transactional
    public Event createEvent(Event event, Organizer organizer) {
        return createEvent(event, organizer, List.of());
    }

    @Transactional
    public Event createEvent(Event event, Organizer organizer, List<Distributor> distributors) {
        event.setOrganizer(organizer);
        Event savedEvent = eventRepository.save(event);
        assignDistributorsToEvent(savedEvent, distributors);
        notificationService.notifyDistributorsOfNewEvent(savedEvent, distributors);
        return savedEvent;
    }

    @Transactional
    public Event updateEvent(Long id, Event updatedEvent, Organizer organizer) {
        return updateEvent(id, updatedEvent, organizer, List.of());
    }

    @Transactional
    public Event updateEvent(Long id, Event updatedEvent, Organizer organizer, List<Distributor> distributors) {
        Event existing = eventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Event not found"));

        // update fields
        existing.setName(updatedEvent.getName());
        existing.setCategory(updatedEvent.getCategory());
        existing.setTicketLimit(updatedEvent.getTicketLimit());
        existing.setEventDate(updatedEvent.getEventDate());
        existing.setLocation(updatedEvent.getLocation());
        existing.setDescription(updatedEvent.getDescription());
        existing.setStatus(updatedEvent.getStatus());
        existing.setCapacity(updatedEvent.getCapacity());
        // Only overwrite image path when an updated image was provided. If null, keep existing image.
        if (updatedEvent.getImagePath() != null && !updatedEvent.getImagePath().isBlank()) {
            existing.setImagePath(updatedEvent.getImagePath());
        }

        mergeSeatTypes(existing, updatedEvent.getSeatTypes());

        existing.setOrganizer(organizer);
        Event savedEvent = eventRepository.save(existing);
        updateEventDistributors(savedEvent, distributors);
        notificationService.notifyDistributorsOfNewEvent(savedEvent, distributors);
        return savedEvent;
    }

    public void deleteEventById(Long id) {
        eventRepository.deleteById(id);
    }

    public List<Event> getEventsByOrganizer(Organizer organizer) {
        return eventRepository.findByOrganizer(organizer);
    }

    public List<Event> getEventsByDistributor(Distributor distributor) {
        if (distributor == null) {
            return List.of();
        }
        return eventRepository.findEventsByDistributor(distributor);
    }

    public List<Distributor> getAssignedDistributors(Long eventId) {
        if (eventId == null) {
            return List.of();
        }
        return eventDistributorRepository.findDistributorsByEventId(eventId);
    }

    public void assignDistributorsToEvent(Event event, List<Distributor> distributors) {
        if (event == null || event.getId() == null || distributors == null || distributors.isEmpty()) {
            return;
        }

        List<EventDistributor> assignments = new ArrayList<>();
        for (Distributor distributor : distinctDistributors(distributors)) {
            assignments.add(new EventDistributor(event, distributor));
        }
        eventDistributorRepository.saveAll(assignments);
    }

    public void updateEventDistributors(Event event, List<Distributor> distributors) {
        if (event == null || event.getId() == null) {
            return;
        }
        eventDistributorRepository.deleteByEvent(event);
        assignDistributorsToEvent(event, distributors);
    }

    /**
     * Merges the seat types submitted by the organizer form into the persisted event,
     * matching on seat category. Rows are updated in place rather than replaced so that
     * the seat type id — and with it the sold seat counter and the ticket sale items
     * referencing it — survive the edit.
     */
    private void mergeSeatTypes(Event existing, List<SeatType> submitted) {
        if (existing.getSeatTypes() == null) {
            existing.setSeatTypes(new ArrayList<>());
        }

        Map<SeatCategory, SeatType> submittedByCategory = new LinkedHashMap<>();
        if (submitted != null) {
            for (SeatType st : submitted) {
                submittedByCategory.put(st.getSeatCategory(), st);
            }
        }

        // Update or remove what the event already has.
        Iterator<SeatType> iterator = existing.getSeatTypes().iterator();
        while (iterator.hasNext()) {
            SeatType current = iterator.next();
            SeatType incoming = submittedByCategory.remove(current.getSeatCategory());

            if (incoming == null) {
                if (current.getSoldSeats() > 0) {
                    throw new IllegalStateException(String.format(
                            "Cannot remove %s — %d ticket(s) have already been sold for it.",
                            current.getSeatCategory().getDisplayName(), current.getSoldSeats()));
                }
                iterator.remove();
                continue;
            }

            if (incoming.getTotalSeats() != null && incoming.getTotalSeats() < current.getSoldSeats()) {
                throw new IllegalStateException(String.format(
                        "Cannot reduce %s to %d seats — %d have already been sold.",
                        current.getSeatCategory().getDisplayName(),
                        incoming.getTotalSeats(), current.getSoldSeats()));
            }
            current.setPrice(incoming.getPrice());
            current.setTotalSeats(incoming.getTotalSeats());
        }

        // Whatever is left was not on the event before.
        for (SeatType incoming : submittedByCategory.values()) {
            incoming.setEvent(existing);
            existing.getSeatTypes().add(incoming);
        }
    }

    private List<Distributor> distinctDistributors(List<Distributor> distributors) {
        Map<Long, Distributor> distinctById = new LinkedHashMap<>();
        for (Distributor distributor : distributors) {
            if (distributor != null && distributor.getId() != null) {
                distinctById.put(distributor.getId(), distributor);
            }
        }
        return new ArrayList<>(distinctById.values());
    }
}
