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
}
