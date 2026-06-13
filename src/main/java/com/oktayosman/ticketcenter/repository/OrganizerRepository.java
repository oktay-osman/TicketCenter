
package com.oktayosman.ticketcenter.repository;

import org.springframework.stereotype.Repository;

/**
 * Marker repository kept for compatibility. Use {@link com.oktayosman.ticketcenter.service.OrganizerService}
 * to access legacy organizer data (organizer_id) via JDBC.
 */
@Repository
public interface OrganizerRepository {

}



