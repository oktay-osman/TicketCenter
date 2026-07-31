package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.Notification;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.model.TicketSale;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.repository.DistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import com.oktayosman.ticketcenter.repository.NotificationRepository;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class NotificationService {

    private static final String ORGANIZER_ROLE_NAME = "ORGANIZER";
    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";

    private final NotificationRepository notificationRepository;
    private final TicketSaleRepository ticketSaleRepository;
    private final EventRepository eventRepository;
    private final OrganizerRepository organizerRepository;
    private final DistributorRepository distributorRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               TicketSaleRepository ticketSaleRepository,
                               EventRepository eventRepository,
                               OrganizerRepository organizerRepository,
                               DistributorRepository distributorRepository) {
        this.notificationRepository = notificationRepository;
        this.ticketSaleRepository = ticketSaleRepository;
        this.eventRepository = eventRepository;
        this.organizerRepository = organizerRepository;
        this.distributorRepository = distributorRepository;
    }

    @Transactional
    public void notifyDistributorsOfNewEvent(Event event, List<Distributor> distributors) {
        if (event == null || distributors == null || distributors.isEmpty()) {
            return;
        }

        for (Distributor distributor : distributors) {
            if (distributor == null || distributor.getUser() == null) {
                continue;
            }
            String message = "New event assignment: '" + event.getName() + "' on "
                    + event.getEventDate().toLocalDate() + ".";
            createNotification(distributor.getUser(), event, message);
        }
    }

    @Transactional
    public void notifyOrganizerTicketsSold(TicketSale ticketSale) {
        if (ticketSale == null || ticketSale.getEvent() == null || ticketSale.getEvent().getOrganizer() == null) {
            return;
        }

        Event event = ticketSale.getEvent();
        Organizer organizer = event.getOrganizer();
        if (organizer.getUser() == null) {
            return;
        }

        Long soldTickets = ticketSaleRepository.getTicketsSoldForEvent(event);
        String distributorName = ticketSale.getDistributor() != null ? ticketSale.getDistributor().getUsername() : "a distributor";
        String message = "Ticket sale update for '" + event.getName() + "': " + soldTickets
                + " tickets sold so far (latest sale by " + distributorName + ").";
        createNotification(organizer.getUser(), event, message);
    }

    @Transactional
    public void notifyUpcomingEventUnsoldTickets(User user) {
        if (user == null || user.getRole() == null || user.getRole().getName() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upcomingLimit = now.plusDays(7);

        List<Event> events = resolveEventsForUser(user);
        for (Event event : events) {
            if (event == null || event.getEventDate() == null) {
                continue;
            }

            if (event.getEventDate().isBefore(now) || event.getEventDate().isAfter(upcomingLimit)) {
                continue;
            }

            long soldTickets = ticketSaleRepository.getTicketsSoldForEvent(event);
            int unsold = Math.max(0, event.getCapacity() - (int) soldTickets);
            if (unsold <= 0) {
                continue;
            }

            String message = "Upcoming event alert: '" + event.getName() + "' is in less than 7 days with "
                    + unsold + " unsold tickets.";
            createNotificationIfNotUnreadDuplicate(user, event, message);
        }
    }

    public List<Notification> getUnreadNotifications(User recipient) {
        if (recipient == null) {
            return List.of();
        }
        return notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(recipient);
    }

    public List<Notification> getAllNotifications(User recipient) {
        if (recipient == null) {
            return List.of();
        }
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found."));

        if (notification.getRecipient() == null || notification.getRecipient().getId() == null
                || !notification.getRecipient().getId().equals(userId)) {
            throw new IllegalStateException("You can only update your own notifications.");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User recipient) {
        if (recipient == null) {
            return;
        }
        List<Notification> unreadNotifications = notificationRepository.findByRecipientAndReadFalseOrderByCreatedAtDesc(recipient);
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    private List<Event> resolveEventsForUser(User user) {
        String roleName = user.getRole().getName();
        if (ORGANIZER_ROLE_NAME.equals(roleName)) {
            return organizerRepository.findById(user.getId())
                    .map(eventRepository::findByOrganizer)
                    .orElse(Collections.emptyList());
        }

        if (DISTRIBUTOR_ROLE_NAME.equals(roleName)) {
            return distributorRepository.findByUser_IdAndUser_Role_Name(user.getId(), DISTRIBUTOR_ROLE_NAME)
                    .map(eventRepository::findEventsByDistributor)
                    .orElse(Collections.emptyList());
        }

        return Collections.emptyList();
    }

    private void createNotification(User recipient, Event event, String message) {
        Notification notification = new Notification(message, recipient);
        notification.setEvent(event);
        notificationRepository.save(notification);
    }

    private void createNotificationIfNotUnreadDuplicate(User recipient, Event event, String message) {
        boolean exists = notificationRepository.existsByRecipientAndEventAndMessageAndReadFalse(recipient, event, message);
        if (!exists) {
            createNotification(recipient, event, message);
        }
    }
}


