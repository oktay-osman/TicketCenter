package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event createEvent(Event event, Organizer organizer) {
        event.setOrganizer(organizer);
        return eventRepository.save(event);
    }

    public Event updateEvent(Long id, Event updatedEvent, Organizer organizer) {
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

        existing.setOrganizer(organizer);
        return eventRepository.save(existing);
    }

    public void deleteEventById(Long id) {
        eventRepository.deleteById(id);
    }

    public List<Event> getEventsByOrganizer(Organizer organizer) {
        return eventRepository.findByOrganizer(organizer);
    }
}
