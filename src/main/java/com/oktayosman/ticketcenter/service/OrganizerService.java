package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OrganizerService {
    private final OrganizerRepository organizerRepository;

    public OrganizerService(OrganizerRepository organizerRepository) {
        this.organizerRepository = organizerRepository;
    }

    public Optional<Organizer> getOrganizerByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return organizerRepository.findById(userId);
    }

    @Transactional
    public Organizer updateOrganizerProfile(Long userId, String organizationName) {
        Organizer organizer = organizerRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Organizer profile not found for user id: " + userId));

        organizer.setOrganizationName(organizationName != null ? organizationName.trim() : "");
        return organizerRepository.save(organizer);
    }
}

