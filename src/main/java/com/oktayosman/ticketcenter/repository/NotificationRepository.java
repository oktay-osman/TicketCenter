package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.Notification;
import com.oktayosman.ticketcenter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    List<Notification> findByRecipientAndReadFalseOrderByCreatedAtDesc(User recipient);

    boolean existsByRecipientAndEventAndMessageAndReadFalse(User recipient, Event event, String message);
}

