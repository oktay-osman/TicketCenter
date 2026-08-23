package com.oktayosman.ticketcenter.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brings the inventory columns introduced with ticket inventory enforcement into a
 * consistent state at startup. Hibernate's ddl-auto=update adds sold_seats and version
 * to existing tables but leaves the values NULL, which breaks the first update of any
 * pre-existing row. This runs before the JavaFX UI is loaded, because the Spring context
 * is started from TicketCenterApplication.init().
 */
@Service
public class InventorySyncService implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Backfills the new columns and recomputes sold_seats from the ticket sale items,
     * which are the authoritative record. Idempotent, so it also repairs any drift.
     * Annotated here rather than on a helper method so the transaction actually applies:
     * Spring calls run() through the proxy, a self-call would not be intercepted.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        entityManager.createNativeQuery(
                        "UPDATE events SET version = 0 WHERE version IS NULL")
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE seat_types SET sold_seats = 0 WHERE sold_seats IS NULL")
                .executeUpdate();

        entityManager.createNativeQuery(
                        "UPDATE seat_types st SET sold_seats = COALESCE(("
                                + "  SELECT SUM(i.quantity) FROM ticket_sales_items i"
                                + "  WHERE i.seat_type_id = st.id"
                                + "), 0)")
                .executeUpdate();
    }
}
