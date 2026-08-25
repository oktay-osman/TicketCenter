Refactoring & Cleanup Backlog

Found during a full-codebase pass looking for business logic in the wrong layer, duplication, and general code health. Ordered roughly by value/effort, not severity — pick items off top to bottom. Each item lists status so we can track progress as we work through them.

Item 1 — Delete dead legacy DB access [DONE]
hibernate/DBUtil.java (a raw Hibernate SessionFactory bootstrap) was never referenced anywhere in the app — pure leftover from a pre-Spring-Data version. Deleted in commit 1f79589. CLAUDE.md's "Legacy Code" bullet referencing DBUtil and JdbcTemplate-in-OrganizerService was also stale (OrganizerService never actually used JdbcTemplate) and has been removed.

Item 2 — Duplicated report date-range filtering [DONE]
OrganizerDashboardController.loadOrganizerReport() and AdminReportsController.loadEventsReport() independently implemented identical from/to date filtering over a List<Event>. Consolidated into EventService.getEventsInRange(Organizer organizer, LocalDate from, LocalDate to) (organizer == null means "all events"), with the filtering itself in a private filterByDateRange helper. Both controllers now call the same method; AdminReportsController got EventService constructor-injected for this.

Item 3 — N+1 queries in report screens [DONE]
Added grouped aggregate queries to TicketSaleRepository (getTicketsSoldGroupedByEvent, getRevenueGroupedByEvent, getTicketsSoldGroupedByDistributorInRange, getRevenueGroupedByDistributorInRange) and two batched service methods on DistributorService — getSalesTotalsByEvent(List<Event>) and getSalesTotalsByDistributors(List<Distributor>, from, to) — returning Map<Long, EventSalesTotals>/Map<Long, DistributorSalesTotals> records. All three report loaders (organizer report, admin events report, admin distributors report) now run one batched call instead of querying per row. This also removed a duplicate pair of methods on AdminDashboardService (getTicketsSoldForEvent/getRevenueForEvent) that were thin wrappers around the same repository queries DistributorService already wrapped — AdminReportsController's events report now goes through DistributorService like the organizer report does.

Item 4 — Business logic trapped in controllers
OrganizerDashboardController is 1,004 lines. handleCreateEvent() hand-parses seat-type rows by walking raw JavaFX HBox/ComboBox/TextField children and casting them, runs full form validation, builds SeatType entities, and does image upload file I/O (directory creation, UUID naming, file copy) directly in the controller. It also leaves a debug System.out.println dumping session/organizer IDs. DistributorCreateSaleController duplicates pricing math (unitPrice * quantity) that the service already recomputes authoritatively, and uses a raw new Thread(() -> Thread.sleep(1000)) to delay closing a dialog instead of a JavaFX PauseTransition. Move validation, seat-type construction, and image handling into EventService (or a small mapper); swap the raw Thread for PauseTransition.

Item 5 — Layering crack in AdminDashboardService
ensureDistributorRecord uses a raw entityManager.createNativeQuery(... ON CONFLICT ... DO NOTHING) upsert — the only native SQL in an otherwise all-JPA codebase, Postgres-specific, and an unexplained exception to the read-then-write pattern ensureOrganizerRecord uses right next to it. Since its only caller (updateUserRole) is already @Transactional, plain findByUser_Id(...).orElseGet(() -> save(new Distributor(...))) works fine and matches the sibling method. Separately, ensureOrganizerRecord is private and annotated @Transactional — Spring's proxy can't intercept a private/self-invoked call, so the annotation is a no-op (harmless today only because its caller is already transactional). Drop the annotation from the private method; it's misleading as-is.

Item 6 — Zero test coverage
pom.xml declares junit-jupiter-api/engine but there is no src/test directory at all. Highest-value first tests: DistributorService.createTicketSale (pessimistic locking order, per-person ticket limit, event capacity, price derived from persisted seat type not the form), EventService.mergeSeatTypes (throws on removing a sold-out category, throws on shrinking below sold count), and AdminDashboardService.updateUserRole (profile cleanup/creation across role transitions).

Item 7 — Inconsistent notification triggering
NotificationService.sendPeriodicSalesDigests() runs on a real @Scheduled hourly cron. notifyUpcomingEventUnsoldTickets(user) instead runs as a side effect inside OrganizerDashboardController.loadNotifications(), which fires on every tab-select and every Refresh click. Deduplicated so it's not currently buggy, but one notification type is time-driven and the other is UI-navigation-driven for no functional reason — an organizer who never opens the tab never gets it; one who mashes Refresh triggers repeated scan-and-maybe-write. Move it onto the same scheduled cadence as the sales digest.

Item 8 — Debug prints left in controllers
17 System.out.println/printStackTrace calls across 6 controllers. Replace with logging/LogUtil or delete where they were clearly one-off debugging (e.g. the organizer/session ID dump in OrganizerDashboardController.handleCreateEvent).
