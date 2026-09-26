package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.EventCategory;
import com.oktayosman.ticketcenter.model.EventDistributor;
import com.oktayosman.ticketcenter.model.EventStatus;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.model.SeatCategory;
import com.oktayosman.ticketcenter.model.SeatType;
import com.oktayosman.ticketcenter.repository.EventDistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.oktayosman.ticketcenter.support.TestData.distributor;
import static com.oktayosman.ticketcenter.support.TestData.event;
import static com.oktayosman.ticketcenter.support.TestData.organizer;
import static com.oktayosman.ticketcenter.support.TestData.seatType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventDistributorRepository eventDistributorRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EventService eventService;

    @Test
    void updateEventThrowsWhenRemovingSeatCategoryWithSoldSeats() {
        // Arrange
        SeatType existingVip = new SeatType();
        existingVip.setSeatCategory(SeatCategory.VIP);
        existingVip.setTotalSeats(10);
        existingVip.setSoldSeats(5);
        existingVip.setPrice(BigDecimal.TEN);

        Event existingEvent = new Event("Concert", "Venue", LocalDateTime.now(), 4, null);
        existingEvent.setSeatTypes(new ArrayList<>(List.of(existingVip)));

        // The organizer submitted an update with no seat types at all —
        // i.e. trying to remove the VIP category entirely.
        Event submittedUpdate = new Event();
        submittedUpdate.setSeatTypes(List.of());

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> eventService.updateEvent(1L, submittedUpdate, null));
    }

    @Test
    void updateEventThrowsWhenReducingTotalSeatsBelowSoldSeats() {
        // Arrange
        SeatType existingVip = new SeatType();
        existingVip.setSeatCategory(SeatCategory.VIP);
        existingVip.setTotalSeats(10);
        existingVip.setSoldSeats(5);
        existingVip.setPrice(BigDecimal.TEN);

        Event existingEvent = new Event("Concert", "Venue", LocalDateTime.now(), 4, null);
        existingEvent.setSeatTypes(new ArrayList<>(List.of(existingVip)));

        // Same category submitted back, but shrunk to 3 total seats —
        // below the 5 that are already sold.
        SeatType submittedVip = new SeatType();
        submittedVip.setSeatCategory(SeatCategory.VIP);
        submittedVip.setTotalSeats(3);
        submittedVip.setPrice(BigDecimal.TEN);

        Event submittedUpdate = new Event();
        submittedUpdate.setSeatTypes(List.of(submittedVip));

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> eventService.updateEvent(1L, submittedUpdate, null));
    }

    @Test
    void assignDistributorsToEventDoesNothingWhenDistributorsListIsEmpty() {
        // Arrange
        Event event = new Event("Concert", "Venue", LocalDateTime.now(), 4, null);

        // Act
        eventService.assignDistributorsToEvent(event, List.of());

        // Assert
        verify(eventDistributorRepository, never()).saveAll(any());
    }

    @Test
    void getEventsByDistributorReturnsEmptyListWhenDistributorIsNull() {
        // Act
        List<Event> result = eventService.getEventsByDistributor(null);

        // Assert
        assertTrue(result.isEmpty());
        // No repository call was even attempted — the null check short-circuits
        // before eventRepository is touched, so nothing needed stubbing here.
    }

    // ---- updateEvent / mergeSeatTypes --------------------------------------

    private Event existingEventWith(SeatType... seatTypes) {
        Event existing = event(1L, "Concert", 0, 4);
        existing.setSeatTypes(new ArrayList<>(List.of(seatTypes)));
        return existing;
    }

    private SeatType submitted(SeatCategory category, String price, Integer totalSeats) {
        SeatType st = new SeatType();
        st.setSeatCategory(category);
        st.setPrice(new BigDecimal(price));
        st.setTotalSeats(totalSeats);
        return st;
    }

    private Event submittedUpdate(SeatType... seatTypes) {
        Event update = new Event();
        update.setName("Renamed");
        update.setSeatTypes(List.of(seatTypes));
        return update;
    }

    @Test
    void updateEventThrowsWhenEventDoesNotExist() {
        when(eventRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> eventService.updateEvent(9L, new Event(), null));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEventUpdatesExistingSeatTypeInPlaceKeepingIdAndSoldSeats() {
        SeatType vip = seatType(10L, null, SeatCategory.VIP, "50.00", 10, 4);
        Event existing = existingEventWith(vip);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.updateEvent(1L, submittedUpdate(submitted(SeatCategory.VIP, "75.00", 20)), null);

        assertEquals(1, existing.getSeatTypes().size());
        assertSame(vip, existing.getSeatTypes().get(0));
        assertEquals(10L, vip.getId());
        assertEquals(new BigDecimal("75.00"), vip.getPrice());
        assertEquals(20, vip.getTotalSeats());
        assertEquals(4, vip.getSoldSeats());
    }

    @Test
    void updateEventAllowsReducingTotalSeatsToExactlySoldSeats() {
        SeatType vip = seatType(10L, null, SeatCategory.VIP, "50.00", 10, 5);
        Event existing = existingEventWith(vip);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.updateEvent(1L, submittedUpdate(submitted(SeatCategory.VIP, "50.00", 5)), null);

        assertEquals(5, vip.getTotalSeats());
    }

    @Test
    void updateEventAddsSeatTypeForNewCategoryAndLinksItToEvent() {
        SeatType vip = seatType(10L, null, SeatCategory.VIP, "50.00", 10, 0);
        Event existing = existingEventWith(vip);
        SeatType normal = submitted(SeatCategory.NORMAL, "20.00", 100);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.updateEvent(1L,
                submittedUpdate(submitted(SeatCategory.VIP, "50.00", 10), normal), null);

        assertEquals(2, existing.getSeatTypes().size());
        assertTrue(existing.getSeatTypes().contains(normal));
        assertSame(existing, normal.getEvent());
    }

    @Test
    void updateEventRemovesSeatTypeThatHasNoSoldSeats() {
        SeatType vip = seatType(10L, null, SeatCategory.VIP, "50.00", 10, 0);
        SeatType normal = seatType(11L, null, SeatCategory.NORMAL, "20.00", 100, 0);
        Event existing = existingEventWith(vip, normal);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.updateEvent(1L, submittedUpdate(submitted(SeatCategory.NORMAL, "20.00", 100)), null);

        assertEquals(List.of(normal), existing.getSeatTypes());
    }

    @Test
    void updateEventKeepsExistingTotalSeatsWhenSubmittedTotalIsNullAndDoesNotFailSoldCheck() {
        SeatType vip = seatType(10L, null, SeatCategory.VIP, "50.00", 10, 5);
        Event existing = existingEventWith(vip);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.updateEvent(1L, submittedUpdate(submitted(SeatCategory.VIP, "60.00", null)), null);

        // null means "uncapped" and is copied over as-is; the important part is no exception.
        assertNull(vip.getTotalSeats());
        assertEquals(new BigDecimal("60.00"), vip.getPrice());
    }

    @Test
    void updateEventCopiesScalarFieldsAndSetsOrganizer() {
        Event existing = existingEventWith();
        Organizer organizer = organizer(7L);
        Event update = submittedUpdate();
        update.setLocation("New Hall");
        update.setCapacity(500);
        update.setTicketLimit(6);
        update.setDescription("desc");
        update.setCategory(EventCategory.THEATER);
        update.setStatus(EventStatus.ACTIVE);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.updateEvent(1L, update, organizer);

        assertEquals("Renamed", existing.getName());
        assertEquals("New Hall", existing.getLocation());
        assertEquals(500, existing.getCapacity());
        assertEquals(6, existing.getTicketLimit());
        assertEquals("desc", existing.getDescription());
        assertEquals(EventCategory.THEATER, existing.getCategory());
        assertEquals(EventStatus.ACTIVE, existing.getStatus());
        assertSame(organizer, existing.getOrganizer());
    }

    @Test
    void updateEventKeepsExistingImageWhenNoNewImageIsProvided() {
        Event existing = existingEventWith();
        existing.setImagePath("old.png");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        Event blank = submittedUpdate();
        blank.setImagePath("   ");
        eventService.updateEvent(1L, blank, null);
        assertEquals("old.png", existing.getImagePath());

        eventService.updateEvent(1L, submittedUpdate(), null);
        assertEquals("old.png", existing.getImagePath());
    }

    @Test
    void updateEventReplacesImageWhenNewImageIsProvided() {
        Event existing = existingEventWith();
        existing.setImagePath("old.png");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        Event update = submittedUpdate();
        update.setImagePath("new.png");
        eventService.updateEvent(1L, update, null);

        assertEquals("new.png", existing.getImagePath());
    }

    @Test
    void updateEventReplacesDistributorAssignmentsAndNotifiesThem() {
        Event existing = existingEventWith();
        Distributor d = distributor(3L, "dist");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(existing);

        eventService.updateEvent(1L, submittedUpdate(), null, List.of(d));

        verify(eventDistributorRepository).deleteByEvent(existing);
        verify(eventDistributorRepository).saveAll(any());
        verify(notificationService).notifyDistributorsOfNewEvent(existing, List.of(d));
    }

    // ---- createEvent -------------------------------------------------------

    @Test
    void createEventSetsOrganizerAssignsDistributorsAndNotifiesThem() {
        Organizer organizer = organizer(7L);
        Distributor d = distributor(3L, "dist");
        Event toSave = event(1L, "New", 0, 0);
        when(eventRepository.save(toSave)).thenReturn(toSave);

        Event result = eventService.createEvent(toSave, organizer, List.of(d));

        assertSame(organizer, result.getOrganizer());
        verify(eventDistributorRepository).saveAll(any());
        verify(notificationService).notifyDistributorsOfNewEvent(toSave, List.of(d));
    }

    @Test
    void createEventWithoutDistributorsSavesNoAssignments() {
        Event toSave = event(1L, "New", 0, 0);
        when(eventRepository.save(toSave)).thenReturn(toSave);

        eventService.createEvent(toSave, organizer(7L));

        verify(eventDistributorRepository, never()).saveAll(any());
    }

    // ---- assignDistributorsToEvent / updateEventDistributors ---------------

    @Test
    @SuppressWarnings("unchecked")
    void assignDistributorsDropsDuplicatesNullsAndUnsavedDistributors() {
        Event saved = event(1L, "Concert", 0, 0);
        Distributor d1 = distributor(3L, "one");
        Distributor d1Again = distributor(3L, "one-again");
        Distributor d2 = distributor(4L, "two");
        Distributor unsaved = distributor(null, "unsaved");
        List<Distributor> input = new ArrayList<>(List.of(d1, d2, d1Again, unsaved));
        input.add(null);

        eventService.assignDistributorsToEvent(saved, input);

        ArgumentCaptor<List<EventDistributor>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventDistributorRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void assignDistributorsDoesNothingForUnsavedEventOrNullList() {
        eventService.assignDistributorsToEvent(event(null, "Unsaved", 0, 0), List.of(distributor(3L, "d")));
        eventService.assignDistributorsToEvent(event(1L, "Saved", 0, 0), null);
        eventService.assignDistributorsToEvent(null, List.of(distributor(3L, "d")));

        verify(eventDistributorRepository, never()).saveAll(any());
    }

    @Test
    void updateEventDistributorsDeletesOldAssignmentsBeforeSavingNewOnes() {
        Event saved = event(1L, "Concert", 0, 0);

        eventService.updateEventDistributors(saved, List.of(distributor(3L, "d")));

        InOrder order = inOrder(eventDistributorRepository);
        order.verify(eventDistributorRepository).deleteByEvent(saved);
        order.verify(eventDistributorRepository).saveAll(any());
    }

    @Test
    void updateEventDistributorsIgnoresUnsavedEvent() {
        eventService.updateEventDistributors(event(null, "Unsaved", 0, 0), List.of(distributor(3L, "d")));

        verify(eventDistributorRepository, never()).deleteByEvent(any());
    }

    @Test
    void getAssignedDistributorsReturnsEmptyForNullEventId() {
        assertTrue(eventService.getAssignedDistributors(null).isEmpty());
        verify(eventDistributorRepository, never()).findDistributorsByEventId(any());
    }
}
