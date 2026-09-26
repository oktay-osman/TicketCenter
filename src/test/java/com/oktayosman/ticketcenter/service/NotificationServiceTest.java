package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.Notification;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.repository.DistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import com.oktayosman.ticketcenter.repository.NotificationRepository;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.oktayosman.ticketcenter.support.TestData.distributor;
import static com.oktayosman.ticketcenter.support.TestData.event;
import static com.oktayosman.ticketcenter.support.TestData.notification;
import static com.oktayosman.ticketcenter.support.TestData.organizer;
import static com.oktayosman.ticketcenter.support.TestData.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private TicketSaleRepository ticketSaleRepository;
    @Mock private EventRepository eventRepository;
    @Mock private OrganizerRepository organizerRepository;
    @Mock private DistributorRepository distributorRepository;

    @InjectMocks
    private NotificationService service;

    private Notification savedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }

    private Event eventInDays(long days, int capacity) {
        Event e = event(1L, "Concert", capacity, 0);
        e.setEventDate(LocalDateTime.now().plusDays(days));
        return e;
    }

    // ---- notifyDistributorsOfNewEvent --------------------------------------

    @Test
    void notifyDistributorsOfNewEventDoesNothingForNullEventOrNoDistributors() {
        service.notifyDistributorsOfNewEvent(null, List.of(distributor(1L, "d")));
        service.notifyDistributorsOfNewEvent(eventInDays(5, 10), null);
        service.notifyDistributorsOfNewEvent(eventInDays(5, 10), List.of());

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifyDistributorsOfNewEventCreatesOneNotificationPerDistributorWithUser() {
        Event e = eventInDays(5, 10);
        Distributor d1 = distributor(1L, "one");
        Distributor d2 = distributor(2L, "two");
        Distributor withoutUser = new Distributor();
        List<Distributor> distributors = new ArrayList<>(List.of(d1, d2, withoutUser));
        distributors.add(null);

        service.notifyDistributorsOfNewEvent(e, distributors);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        Notification first = captor.getAllValues().get(0);
        assertSame(d1.getUser(), first.getRecipient());
        assertSame(e, first.getEvent());
        assertTrue(first.getMessage().contains("Concert"));
        assertTrue(first.getMessage().contains(e.getEventDate().toLocalDate().toString()));
        assertFalse(first.isRead());
    }

    // ---- notifyUpcomingEventUnsoldTickets ----------------------------------

    @Test
    void upcomingAlertIgnoresNullUserAndUserWithoutRole() {
        service.notifyUpcomingEventUnsoldTickets(null);
        service.notifyUpcomingEventUnsoldTickets(user(1L, "u", null));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void upcomingAlertForOrganizerNotifiesAboutEventWithUnsoldTicketsInsideSevenDays() {
        User u = user(7L, "org", "ORGANIZER");
        Organizer o = organizer(7L);
        Event e = eventInDays(3, 100);
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(o));
        when(eventRepository.findByOrganizer(o)).thenReturn(List.of(e));
        when(ticketSaleRepository.getTicketsSoldForEvent(e)).thenReturn(40L);
        when(notificationRepository.existsByRecipientAndEventAndMessageAndReadFalse(any(), any(), anyString()))
                .thenReturn(false);

        service.notifyUpcomingEventUnsoldTickets(u);

        Notification n = savedNotification();
        assertSame(u, n.getRecipient());
        assertTrue(n.getMessage().contains("60 unsold tickets"), n.getMessage());
    }

    @Test
    void upcomingAlertForDistributorUsesDistributorsEvents() {
        User u = user(9L, "dist", "DISTRIBUTOR");
        Distributor d = distributor(3L, "dist");
        Event e = eventInDays(2, 10);
        when(distributorRepository.findByUser_IdAndUser_Role_Name(9L, "DISTRIBUTOR")).thenReturn(Optional.of(d));
        when(eventRepository.findEventsByDistributor(d)).thenReturn(List.of(e));
        when(ticketSaleRepository.getTicketsSoldForEvent(e)).thenReturn(0L);

        service.notifyUpcomingEventUnsoldTickets(u);

        assertSame(u, savedNotification().getRecipient());
    }

    @Test
    void upcomingAlertSkipsEventsOutsideTheSevenDayWindow() {
        User u = user(7L, "org", "ORGANIZER");
        Organizer o = organizer(7L);
        Event past = eventInDays(-1, 100);
        Event far = eventInDays(10, 100);
        Event noDate = event(2L, "No date", 100, 0);
        noDate.setEventDate(null);
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(o));
        when(eventRepository.findByOrganizer(o)).thenReturn(List.of(past, far, noDate));

        service.notifyUpcomingEventUnsoldTickets(u);

        verify(notificationRepository, never()).save(any());
        verify(ticketSaleRepository, never()).getTicketsSoldForEvent(any());
    }

    @Test
    void upcomingAlertSkipsSoldOutEvents() {
        User u = user(7L, "org", "ORGANIZER");
        Organizer o = organizer(7L);
        Event e = eventInDays(3, 100);
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(o));
        when(eventRepository.findByOrganizer(o)).thenReturn(List.of(e));
        when(ticketSaleRepository.getTicketsSoldForEvent(e)).thenReturn(100L);

        service.notifyUpcomingEventUnsoldTickets(u);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void upcomingAlertDoesNotCreateDuplicateWhileAnIdenticalOneIsUnread() {
        User u = user(7L, "org", "ORGANIZER");
        Organizer o = organizer(7L);
        Event e = eventInDays(3, 100);
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(o));
        when(eventRepository.findByOrganizer(o)).thenReturn(List.of(e));
        when(ticketSaleRepository.getTicketsSoldForEvent(e)).thenReturn(10L);
        when(notificationRepository.existsByRecipientAndEventAndMessageAndReadFalse(any(), any(), anyString()))
                .thenReturn(true);

        service.notifyUpcomingEventUnsoldTickets(u);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void upcomingAlertDoesNothingForOtherRoles() {
        service.notifyUpcomingEventUnsoldTickets(user(1L, "u", "USER"));

        verify(notificationRepository, never()).save(any());
        verify(eventRepository, never()).findByOrganizer(any());
    }

    @Test
    void upcomingAlertDoesNothingWhenOrganizerProfileIsMissing() {
        when(organizerRepository.findById(7L)).thenReturn(Optional.empty());

        service.notifyUpcomingEventUnsoldTickets(user(7L, "org", "ORGANIZER"));

        verify(notificationRepository, never()).save(any());
    }

    // ---- sendPeriodicSalesDigests ------------------------------------------

    @Test
    void periodicDigestSendsOneSummaryPerEventWithSales() {
        Event e = event(1L, "Concert", 0, 0);
        Organizer o = organizer(7L);
        e.setOrganizer(o);
        when(ticketSaleRepository.findEventsWithSalesBetween(any(), any())).thenReturn(List.of(e));
        when(ticketSaleRepository.getTicketsSoldForEventAndDateRange(any(), any(), any())).thenReturn(4L);
        when(ticketSaleRepository.getTicketsSoldForEvent(e)).thenReturn(20L);

        service.sendPeriodicSalesDigests();

        Notification n = savedNotification();
        assertSame(o.getUser(), n.getRecipient());
        assertTrue(n.getMessage().contains("4 tickets sold"), n.getMessage());
        assertTrue(n.getMessage().contains("20 total"), n.getMessage());
    }

    @Test
    void periodicDigestSkipsEventsWithoutOrganizerUser() {
        Event noOrganizer = event(1L, "A", 0, 0);
        Event organizerWithoutUser = event(2L, "B", 0, 0);
        organizerWithoutUser.setOrganizer(new Organizer());
        when(ticketSaleRepository.findEventsWithSalesBetween(any(), any()))
                .thenReturn(List.of(noOrganizer, organizerWithoutUser));

        service.sendPeriodicSalesDigests();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void periodicDigestDoesNothingWhenNoEventHadSales() {
        when(ticketSaleRepository.findEventsWithSalesBetween(any(), any())).thenReturn(List.of());

        service.sendPeriodicSalesDigests();

        verify(notificationRepository, never()).save(any());
    }

    // ---- read / unread -----------------------------------------------------

    @Test
    void getNotificationsReturnEmptyForNullRecipient() {
        assertTrue(service.getUnreadNotifications(null).isEmpty());
        assertTrue(service.getAllNotifications(null).isEmpty());
        verify(notificationRepository, never()).findByRecipientOrderByCreatedAtDesc(any());
    }

    @Test
    void getNotificationsDelegateToRepository() {
        User u = user(1L, "u", "USER");
        Notification n = notification(1L, u);
        when(notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(u)).thenReturn(List.of(n));
        when(notificationRepository.findByRecipientOrderByCreatedAtDesc(u)).thenReturn(List.of(n, n));

        assertEquals(1, service.getUnreadNotifications(u).size());
        assertEquals(2, service.getAllNotifications(u).size());
    }

    @Test
    void markAsReadFailsForUnknownNotification() {
        when(notificationRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.markAsRead(5L, 1L));
    }

    @Test
    void markAsReadRejectsAnotherUsersNotification() {
        Notification n = notification(5L, user(1L, "owner", "USER"));
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThrows(IllegalStateException.class, () -> service.markAsRead(5L, 2L));
        assertFalse(n.isRead());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsReadRejectsNotificationWithoutRecipient() {
        Notification n = notification(5L, null);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThrows(IllegalStateException.class, () -> service.markAsRead(5L, 1L));
    }

    @Test
    void markAsReadMarksAndSavesOwnNotification() {
        Notification n = notification(5L, user(1L, "owner", "USER"));
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        service.markAsRead(5L, 1L);

        assertTrue(n.isRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAllAsReadMarksEveryUnreadNotification() {
        User u = user(1L, "u", "USER");
        Notification a = notification(1L, u);
        Notification b = notification(2L, u);
        List<Notification> unread = List.of(a, b);
        when(notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(u)).thenReturn(unread);

        service.markAllAsRead(u);

        assertTrue(a.isRead());
        assertTrue(b.isRead());
        verify(notificationRepository).saveAll(unread);
    }

    @Test
    void markAllAsReadIgnoresNullRecipient() {
        service.markAllAsRead(null);

        verify(notificationRepository, never()).saveAll(any());
    }
}
