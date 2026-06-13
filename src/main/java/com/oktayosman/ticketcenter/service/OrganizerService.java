package com.oktayosman.ticketcenter.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrganizerService {
    private final JdbcTemplate jdbc;

    public OrganizerService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Integer findLegacyOrganizerIdByUserId(Long userId) {
        try {
            return jdbc.queryForObject("SELECT organizer_id FROM organizers WHERE user_id = ?", Integer.class, userId);
        } catch (Exception e) {
            return null;
        }
    }
}

