package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.EventDistributor;
import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.repository.EventDistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oktayosman.ticketcenter.model.SeatType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventDistributorRepository eventDistributorRepository;

    public EventService(EventRepository eventRepository,
                        EventDistributorRepository eventDistributorRepository) {
        this.eventRepository = eventRepository;
        this.eventDistributorRepository = eventDistributorRepository;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<Event> getAllEventsWithOrganizerUser() {
        return eventRepository.findAllWithOrganizerUser();
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
        if (updatedEvent.getOrganizerLegacyId() != null) {
            existing.setOrganizerLegacyId(updatedEvent.getOrganizerLegacyId());
        }

        // Merge seat types: replace the existing collection in-place so that
        // JPA orphanRemoval deletes removed entries and CascadeType.ALL inserts new ones.
        if (existing.getSeatTypes() != null) {
            existing.getSeatTypes().clear();
        }
        if (updatedEvent.getSeatTypes() != null && !updatedEvent.getSeatTypes().isEmpty()) {
            if (existing.getSeatTypes() == null) {
                existing.setSeatTypes(new ArrayList<>());
            }
            for (SeatType st : updatedEvent.getSeatTypes()) {
                st.setEvent(existing);
                existing.getSeatTypes().add(st);
            }
        }

        existing.setOrganizer(organizer);
        Event savedEvent = eventRepository.save(existing);
        updateEventDistributors(savedEvent, distributors);
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
