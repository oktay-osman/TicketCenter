package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;

    @Autowired
    public AdminDashboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
//        this.eventRepository = eventRepository;
//        this.ticketRepository = ticketRepository;
    }

    public int getTotalUsers() {
        return (int) userRepository.count();
    }

    public int getTotalEvents() {
//        return (int) eventRepository.count();
        return 1; // Placeholder until EventRepository is implemented
    }

//    public int getTotalTicketsSold() {
//        return ticketRepository.countTicketsSold();
//    }

//    public double getTotalRevenue() {
//        Double revenue = ticketRepository.calculateTotalRevenue();
//        return revenue != null ? revenue : 0.0;
//    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

}