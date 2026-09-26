package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.exception.TicketInventoryException;
import com.oktayosman.ticketcenter.exception.TicketLimitExceededException;
import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.DistributorRating;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.EventCategory;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.model.SeatCategory;
import com.oktayosman.ticketcenter.model.SeatType;
import com.oktayosman.ticketcenter.model.TicketSale;
import com.oktayosman.ticketcenter.model.TicketSaleItem;
import com.oktayosman.ticketcenter.repository.DistributorRatingRepository;
import com.oktayosman.ticketcenter.repository.DistributorRepository;
import com.oktayosman.ticketcenter.repository.EventDistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import com.oktayosman.ticketcenter.repository.SeatTypeRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleItemRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.oktayosman.ticketcenter.support.TestData.distributor;
import static com.oktayosman.ticketcenter.support.TestData.event;
import static com.oktayosman.ticketcenter.support.TestData.formItem;
import static com.oktayosman.ticketcenter.support.TestData.organizer;
import static com.oktayosman.ticketcenter.support.TestData.seatType;
import static com.oktayosman.ticketcenter.support.TestData.setId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributorServiceTest {

    @Mock private DistributorRepository distributorRepository;
    @Mock private DistributorRatingRepository distributorRatingRepository;
    @Mock private OrganizerRepository organizerRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventDistributorRepository eventDistributorRepository;
    @Mock private TicketSaleRepository ticketSaleRepository;
    @Mock private TicketSaleItemRepository ticketSaleItemRepository;
    @Mock private SeatTypeRepository seatTypeRepository;
    @Mock private EventService eventService;

    @InjectMocks
    private DistributorService service;

    private Event event;
    private SeatType vip;
    private SeatType normal;

    @BeforeEach
    void setUp() {
        event = event(1L, "Concert", 0, 0);
        vip = seatType(10L, event, SeatCategory.VIP, "100.00", 5, 0);
        normal = seatType(11L, event, SeatCategory.NORMAL, "40.00", 100, 0);
    }

    // ---- helpers -----------------------------------------------------------

    private TicketSale saleFor(String buyerEmail, TicketSaleItem... items) {
        Event formEvent = event(1L, "stale name", 0, 0);
        TicketSale sale = new TicketSale(formEvent, distributor(1L, "dist"), "Jane", "Doe", buyerEmail, BigDecimal.ZERO);
        sale.setItems(new ArrayList<>(List.of(items)));
        return sale;
    }

    private void stubEventLock() {
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
    }

    private void stubSeatTypeLocks() {
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        when(seatTypeRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(normal));
    }

    // ---- createTicketSale: input validation --------------------------------

    @Test
    void createTicketSaleRejectsNullItems() {
        TicketSale sale = saleFor("a@b.com");
        sale.setItems(null);

        assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale));
        verify(ticketSaleRepository, never()).save(any());
    }

    @Test
    void createTicketSaleRejectsEmptyItems() {
        TicketSale sale = saleFor("a@b.com");

        assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale));
    }

    @Test
    void createTicketSaleRejectsMissingEvent() {
        TicketSale sale = saleFor("a@b.com", formItem(10L, 1));
        sale.setEvent(null);

        assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale));
    }

    @Test
    void createTicketSaleRejectsEventWithoutId() {
        TicketSale sale = saleFor("a@b.com", formItem(10L, 1));
        sale.setEvent(event(null, "Unsaved", 0, 0));

        assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale));
    }

    @Test
    void createTicketSaleRejectsUnknownEvent() {
        when(eventRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());
        TicketSale sale = saleFor("a@b.com", formItem(10L, 1));

        assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale));
    }

    @Test
    void createTicketSaleRejectsItemWithoutSeatType() {
        stubEventLock();
        TicketSaleItem item = new TicketSaleItem();
        item.setQuantity(1);
        TicketSale sale = saleFor("a@b.com", item);

        assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale));
    }

    @Test
    void createTicketSaleRejectsZeroNegativeAndNullQuantities() {
        stubEventLock();

        for (Integer quantity : new Integer[]{0, -3, null}) {
            TicketSale sale = saleFor("a@b.com", formItem(10L, quantity));
            assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale),
                    "quantity " + quantity + " should be rejected");
        }
        verify(ticketSaleRepository, never()).save(any());
    }

    @Test
    void createTicketSaleRejectsUnknownSeatType() {
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());
        TicketSale sale = saleFor("a@b.com", formItem(10L, 1));

        assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale));
    }

    @Test
    void createTicketSaleRejectsSeatTypeFromAnotherEvent() {
        stubEventLock();
        Event other = event(2L, "Other", 0, 0);
        SeatType foreign = seatType(10L, other, SeatCategory.VIP, "100.00", 5, 0);
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(foreign));
        TicketSale sale = saleFor("a@b.com", formItem(10L, 1));

        assertThrows(IllegalArgumentException.class, () -> service.createTicketSale(sale));
    }

    // ---- createTicketSale: inventory ---------------------------------------

    @Test
    void createTicketSaleRejectsRequestAboveAvailableSeats() {
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        TicketSale sale = saleFor("a@b.com", formItem(10L, 6));

        assertThrows(TicketInventoryException.class, () -> service.createTicketSale(sale));
        assertEquals(0, vip.getSoldSeats());
        verify(ticketSaleRepository, never()).save(any());
    }

    @Test
    void createTicketSaleAllowsBuyingExactlyTheRemainingSeats() {
        stubEventLock();
        vip.setSoldSeats(3);
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        TicketSale sale = saleFor("a@b.com", formItem(10L, 2));
        when(ticketSaleRepository.save(sale)).thenReturn(sale);

        service.createTicketSale(sale);

        assertEquals(5, vip.getSoldSeats());
    }

    @Test
    void createTicketSaleMergesRowsForSameSeatTypeBeforeCheckingAvailability() {
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        // 3 + 3 = 6 > 5 available; each row alone would pass.
        TicketSale sale = saleFor("a@b.com", formItem(10L, 3), formItem(10L, 3));

        assertThrows(TicketInventoryException.class, () -> service.createTicketSale(sale));
    }

    @Test
    void createTicketSaleTreatsNullTotalSeatsAsUncapped() {
        stubEventLock();
        vip.setTotalSeats(null);
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        TicketSale sale = saleFor("a@b.com", formItem(10L, 1000));
        when(ticketSaleRepository.save(sale)).thenReturn(sale);

        service.createTicketSale(sale);

        assertEquals(1000, vip.getSoldSeats());
    }

    // ---- createTicketSale: per-person limit --------------------------------

    @Test
    void createTicketSaleRejectsBuyerAbovePerPersonLimit() {
        event = event(1L, "Concert", 0, 4);
        vip = seatType(10L, event, SeatCategory.VIP, "100.00", 5, 0);
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        when(ticketSaleRepository.getTicketsSoldForEventAndBuyer(event, "a@b.com")).thenReturn(3L);
        TicketSale sale = saleFor("a@b.com", formItem(10L, 2));

        assertThrows(TicketLimitExceededException.class, () -> service.createTicketSale(sale));
        assertEquals(0, vip.getSoldSeats());
    }

    @Test
    void createTicketSaleAllowsBuyerExactlyAtPerPersonLimit() {
        event = event(1L, "Concert", 0, 4);
        vip = seatType(10L, event, SeatCategory.VIP, "100.00", 5, 0);
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        when(ticketSaleRepository.getTicketsSoldForEventAndBuyer(event, "a@b.com")).thenReturn(2L);
        TicketSale sale = saleFor("a@b.com", formItem(10L, 2));
        when(ticketSaleRepository.save(sale)).thenReturn(sale);

        service.createTicketSale(sale);

        assertEquals(2, vip.getSoldSeats());
    }

    @Test
    void createTicketSaleNormalisesBuyerEmailWhenCheckingLimit() {
        event = event(1L, "Concert", 0, 4);
        vip = seatType(10L, event, SeatCategory.VIP, "100.00", 5, 0);
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        when(ticketSaleRepository.getTicketsSoldForEventAndBuyer(event, "jane@example.com")).thenReturn(4L);
        TicketSale sale = saleFor("  Jane@Example.COM ", formItem(10L, 1));

        assertThrows(TicketLimitExceededException.class, () -> service.createTicketSale(sale));
    }

    @Test
    void createTicketSaleTreatsMissingPurchaseHistoryAsZero() {
        event = event(1L, "Concert", 0, 4);
        vip = seatType(10L, event, SeatCategory.VIP, "100.00", 5, 0);
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        when(ticketSaleRepository.getTicketsSoldForEventAndBuyer(event, "a@b.com")).thenReturn(null);
        TicketSale sale = saleFor("a@b.com", formItem(10L, 4));
        when(ticketSaleRepository.save(sale)).thenReturn(sale);

        service.createTicketSale(sale);

        assertEquals(4, vip.getSoldSeats());
    }

    @Test
    void createTicketSaleSkipsPerPersonCheckWhenLimitIsZero() {
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        TicketSale sale = saleFor("a@b.com", formItem(10L, 5));
        when(ticketSaleRepository.save(sale)).thenReturn(sale);

        service.createTicketSale(sale);

        verify(ticketSaleRepository, never()).getTicketsSoldForEventAndBuyer(any(), any());
    }

    // ---- createTicketSale: event capacity ----------------------------------

    @Test
    void createTicketSaleRejectsSaleThatExceedsEventCapacity() {
        event = event(1L, "Concert", 10, 0);
        normal = seatType(11L, event, SeatCategory.NORMAL, "40.00", 100, 0);
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(normal));
        when(ticketSaleRepository.getTicketsSoldForEvent(event)).thenReturn(8L);
        TicketSale sale = saleFor("a@b.com", formItem(11L, 3));

        assertThrows(TicketInventoryException.class, () -> service.createTicketSale(sale));
        assertEquals(0, normal.getSoldSeats());
    }

    @Test
    void createTicketSaleAllowsSaleThatFillsEventCapacityExactly() {
        event = event(1L, "Concert", 10, 0);
        normal = seatType(11L, event, SeatCategory.NORMAL, "40.00", 100, 0);
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(normal));
        when(ticketSaleRepository.getTicketsSoldForEvent(event)).thenReturn(8L);
        TicketSale sale = saleFor("a@b.com", formItem(11L, 2));
        when(ticketSaleRepository.save(sale)).thenReturn(sale);

        service.createTicketSale(sale);

        assertEquals(2, normal.getSoldSeats());
    }

    // ---- createTicketSale: happy path --------------------------------------

    @Test
    void createTicketSalePricesFromPersistedSeatTypesAndClaimsInventory() {
        stubEventLock();
        stubSeatTypeLocks();
        TicketSaleItem vipItem = formItem(10L, 2);
        TicketSaleItem normalItem = formItem(11L, 3);
        // The form claims a bogus price; it must be ignored.
        vipItem.setUnitPrice(new BigDecimal("0.01"));
        TicketSale sale = saleFor("a@b.com", vipItem, normalItem);
        when(ticketSaleRepository.save(sale)).thenReturn(sale);

        TicketSale saved = service.createTicketSale(sale);

        assertSame(sale, saved);
        assertSame(event, sale.getEvent(), "event must be replaced by the freshly locked instance");
        assertEquals(new BigDecimal("100.00"), vipItem.getUnitPrice());
        assertEquals(new BigDecimal("200.00"), vipItem.getSubtotal());
        assertEquals(new BigDecimal("120.00"), normalItem.getSubtotal());
        assertEquals(new BigDecimal("320.00"), sale.getTotalAmount());
        assertSame(vip, vipItem.getSeatType());
        assertSame(sale, vipItem.getTicketSale());
        assertEquals(2, vip.getSoldSeats());
        assertEquals(3, normal.getSoldSeats());
    }

    @Test
    void createTicketSaleIncrementsSoldSeatsOncePerSeatTypeForSplitRows() {
        stubEventLock();
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));
        TicketSale sale = saleFor("a@b.com", formItem(10L, 1), formItem(10L, 2));
        when(ticketSaleRepository.save(sale)).thenReturn(sale);

        service.createTicketSale(sale);

        assertEquals(3, vip.getSoldSeats());
        assertEquals(new BigDecimal("300.00"), sale.getTotalAmount());
    }

    // ---- deleteTicketSale --------------------------------------------------

    @Test
    void deleteTicketSaleDoesNothingWhenSaleDoesNotExist() {
        when(ticketSaleRepository.findById(99L)).thenReturn(Optional.empty());

        service.deleteTicketSale(99L);

        verify(ticketSaleRepository, never()).delete(any());
    }

    @Test
    void deleteTicketSaleReleasesSoldSeatsAndDeletesSale() {
        vip.setSoldSeats(4);
        TicketSaleItem item = formItem(10L, 3);
        TicketSale sale = saleFor("a@b.com", item);
        when(ticketSaleRepository.findById(5L)).thenReturn(Optional.of(sale));
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));

        service.deleteTicketSale(5L);

        assertEquals(1, vip.getSoldSeats());
        verify(ticketSaleRepository).delete(sale);
    }

    @Test
    void deleteTicketSaleNeverDrivesSoldSeatsBelowZero() {
        vip.setSoldSeats(1);
        TicketSale sale = saleFor("a@b.com", formItem(10L, 3));
        when(ticketSaleRepository.findById(5L)).thenReturn(Optional.of(sale));
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(vip));

        service.deleteTicketSale(5L);

        assertEquals(0, vip.getSoldSeats());
    }

    @Test
    void deleteTicketSaleStillDeletesWhenSeatTypeIsGone() {
        TicketSale sale = saleFor("a@b.com", formItem(10L, 3));
        when(ticketSaleRepository.findById(5L)).thenReturn(Optional.of(sale));
        when(seatTypeRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        service.deleteTicketSale(5L);

        verify(ticketSaleRepository).delete(sale);
    }

    @Test
    void deleteTicketSaleHandlesSaleWithNoItems() {
        TicketSale sale = saleFor("a@b.com");
        sale.setItems(null);
        when(ticketSaleRepository.findById(5L)).thenReturn(Optional.of(sale));

        service.deleteTicketSale(5L);

        verify(ticketSaleRepository).delete(sale);
    }

    // ---- rateDistributor ---------------------------------------------------

    private static class RatingFixture {
        Organizer organizer = organizer(7L);
        Event event = event(1L, "Concert", 0, 0);
        Distributor distributor = distributor(3L, "dist");

        RatingFixture() {
            event.setOrganizer(organizer);
        }
    }

    private RatingFixture stubRatingLookups() {
        RatingFixture f = new RatingFixture();
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(f.organizer));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(f.event));
        when(distributorRepository.findById(3L)).thenReturn(Optional.of(f.distributor));
        return f;
    }

    @Test
    void rateDistributorRejectsRatingsOutsideOneToFive() {
        assertThrows(IllegalArgumentException.class, () -> service.rateDistributor(7L, 1L, 3L, 0, null));
        assertThrows(IllegalArgumentException.class, () -> service.rateDistributor(7L, 1L, 3L, 6, null));
        verify(distributorRatingRepository, never()).save(any());
    }

    @Test
    void rateDistributorFailsWhenOrganizerProfileIsMissing() {
        when(organizerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.rateDistributor(7L, 1L, 3L, 4, null));
    }

    @Test
    void rateDistributorFailsWhenEventIsMissing() {
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(organizer(7L)));
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.rateDistributor(7L, 1L, 3L, 4, null));
    }

    @Test
    void rateDistributorFailsWhenDistributorIsMissing() {
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(organizer(7L)));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event(1L, "Concert", 0, 0)));
        when(distributorRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.rateDistributor(7L, 1L, 3L, 4, null));
    }

    @Test
    void rateDistributorRejectsEventOwnedByAnotherOrganizer() {
        RatingFixture f = stubRatingLookups();
        f.event.setOrganizer(organizer(8L));

        assertThrows(IllegalStateException.class, () -> service.rateDistributor(7L, 1L, 3L, 4, null));
        verify(distributorRatingRepository, never()).save(any());
    }

    @Test
    void rateDistributorRejectsEventWithoutOrganizer() {
        RatingFixture f = stubRatingLookups();
        f.event.setOrganizer(null);

        assertThrows(IllegalStateException.class, () -> service.rateDistributor(7L, 1L, 3L, 4, null));
    }

    @Test
    void rateDistributorRejectsDistributorNotAssignedToEvent() {
        RatingFixture f = stubRatingLookups();
        when(eventDistributorRepository.existsByEventAndDistributor(f.event, f.distributor)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> service.rateDistributor(7L, 1L, 3L, 4, null));
        verify(distributorRatingRepository, never()).save(any());
    }

    @Test
    void rateDistributorCreatesNewRatingAndStoresAverageOnDistributor() {
        RatingFixture f = stubRatingLookups();
        when(eventDistributorRepository.existsByEventAndDistributor(f.event, f.distributor)).thenReturn(true);
        when(distributorRatingRepository.findByOrganizerAndDistributorAndEvent(f.organizer, f.distributor, f.event))
                .thenReturn(Optional.empty());
        when(distributorRatingRepository.findAverageByDistributor(f.distributor)).thenReturn(4.5);
        when(distributorRepository.save(f.distributor)).thenReturn(f.distributor);

        Distributor result = service.rateDistributor(7L, 1L, 3L, 5, "  Great work  ");

        org.mockito.ArgumentCaptor<DistributorRating> captor = org.mockito.ArgumentCaptor.forClass(DistributorRating.class);
        verify(distributorRatingRepository).save(captor.capture());
        DistributorRating saved = captor.getValue();
        assertEquals(5, saved.getRatingValue());
        assertEquals("Great work", saved.getComment());
        assertSame(f.organizer, saved.getOrganizer());
        assertSame(f.distributor, saved.getDistributor());
        assertSame(f.event, saved.getEvent());
        assertEquals(4.5f, result.getRating());
    }

    @Test
    void rateDistributorUpdatesExistingRatingInsteadOfCreatingAnother() {
        RatingFixture f = stubRatingLookups();
        DistributorRating existing = new DistributorRating();
        existing.setRatingValue(2);
        when(eventDistributorRepository.existsByEventAndDistributor(f.event, f.distributor)).thenReturn(true);
        when(distributorRatingRepository.findByOrganizerAndDistributorAndEvent(f.organizer, f.distributor, f.event))
                .thenReturn(Optional.of(existing));
        when(distributorRatingRepository.findAverageByDistributor(f.distributor)).thenReturn(4.0);
        when(distributorRepository.save(f.distributor)).thenReturn(f.distributor);

        service.rateDistributor(7L, 1L, 3L, 4, null);

        assertEquals(4, existing.getRatingValue());
        verify(distributorRatingRepository).save(existing);
    }

    @Test
    void rateDistributorStoresBlankCommentAsNull() {
        RatingFixture f = stubRatingLookups();
        DistributorRating existing = new DistributorRating();
        existing.setComment("old");
        when(eventDistributorRepository.existsByEventAndDistributor(f.event, f.distributor)).thenReturn(true);
        when(distributorRatingRepository.findByOrganizerAndDistributorAndEvent(f.organizer, f.distributor, f.event))
                .thenReturn(Optional.of(existing));
        when(distributorRepository.save(f.distributor)).thenReturn(f.distributor);

        service.rateDistributor(7L, 1L, 3L, 3, "   ");

        assertNull(existing.getComment());
    }

    @Test
    void rateDistributorClearsDistributorRatingWhenNoAverageExists() {
        RatingFixture f = stubRatingLookups();
        f.distributor.setRating(3.0f);
        when(eventDistributorRepository.existsByEventAndDistributor(f.event, f.distributor)).thenReturn(true);
        when(distributorRatingRepository.findByOrganizerAndDistributorAndEvent(f.organizer, f.distributor, f.event))
                .thenReturn(Optional.empty());
        when(distributorRatingRepository.findAverageByDistributor(f.distributor)).thenReturn(null);
        when(distributorRepository.save(f.distributor)).thenReturn(f.distributor);

        Distributor result = service.rateDistributor(7L, 1L, 3L, 3, null);

        assertNull(result.getRating());
    }

    // ---- calculateSaleTotal / aggregates -----------------------------------

    @Test
    void calculateSaleTotalIsZeroForNullOrEmptyItems() {
        assertEquals(BigDecimal.ZERO, service.calculateSaleTotal(null));
        assertEquals(BigDecimal.ZERO, service.calculateSaleTotal(List.of()));
    }

    @Test
    void calculateSaleTotalSumsSubtotals() {
        TicketSaleItem a = new TicketSaleItem();
        a.setSubtotal(new BigDecimal("10.50"));
        TicketSaleItem b = new TicketSaleItem();
        b.setSubtotal(new BigDecimal("4.25"));

        assertEquals(new BigDecimal("14.75"), service.calculateSaleTotal(List.of(a, b)));
    }

    @Test
    void addItemToSaleComputesSubtotalFromSeatTypePrice() {
        TicketSale sale = saleFor("a@b.com");
        when(ticketSaleItemRepository.save(any(TicketSaleItem.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketSaleItem item = service.addItemToSale(sale, vip, 3);

        assertEquals(new BigDecimal("100.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("300.00"), item.getSubtotal());
        assertEquals(3, item.getQuantity());
    }

    @Test
    void getTicketsSoldByEventDefaultsToZeroWhenRepositoryReturnsNull() {
        when(ticketSaleRepository.getTicketsSoldForEvent(event)).thenReturn(null);

        assertEquals(0L, service.getTicketsSoldByEvent(event));
    }

    @Test
    void getRevenueByEventDefaultsToZeroWhenRepositoryReturnsNull() {
        when(ticketSaleRepository.getRevenueForEvent(event)).thenReturn(null);

        assertEquals(BigDecimal.ZERO, service.getRevenueByEvent(event));
    }

    // ---- getCategoryBreakdownRows ------------------------------------------

    private TicketSale saleWithCategory(EventCategory category, String total) {
        Event e = event(1L, "E", 0, 0);
        e.setCategory(category);
        return new TicketSale(e, null, "A", "B", "a@b.com", new BigDecimal(total));
    }

    @Test
    void categoryBreakdownShowsPlaceholderForNullOrEmptySales() {
        assertEquals(List.of("No sales in current filter."), service.getCategoryBreakdownRows(null));
        assertEquals(List.of("No sales in current filter."), service.getCategoryBreakdownRows(List.of()));
    }

    @Test
    void categoryBreakdownGroupsBySortedCategoryWithCountAndRevenue() {
        List<String> rows = service.getCategoryBreakdownRows(List.of(
                saleWithCategory(EventCategory.THEATER, "10.00"),
                saleWithCategory(EventCategory.CONCERTS, "20.00"),
                saleWithCategory(EventCategory.CONCERTS, "5.50")));

        assertEquals(2, rows.size());
        assertTrue(rows.get(0).startsWith("CONCERTS"));
        assertTrue(rows.get(0).contains("2 sales"));
        // %.2f follows the default locale, so accept either decimal separator.
        assertTrue(rows.get(0).matches(".*25[.,]50.*"), rows.get(0));
        assertTrue(rows.get(1).startsWith("THEATER"));
        assertTrue(rows.get(1).contains("1 sales"));
    }

    @Test
    void categoryBreakdownLabelsMissingCategoryAsUnknown() {
        List<String> rows = service.getCategoryBreakdownRows(List.of(saleWithCategory(null, "9.00")));

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).startsWith("UNKNOWN"));
    }

    // ---- delegates ---------------------------------------------------------

    @Test
    void getEventsForDistributorDelegatesToEventService() {
        Distributor d = distributor(1L, "dist");
        when(eventService.getEventsByDistributor(d)).thenReturn(List.of(event));

        assertEquals(List.of(event), service.getEventsForDistributor(d));
    }

    @Test
    void getDistributorByUserIdLooksUpByDistributorRole() {
        Distributor d = distributor(1L, "dist");
        when(distributorRepository.findByUser_IdAndUser_Role_Name(5L, "DISTRIBUTOR")).thenReturn(Optional.of(d));

        assertEquals(Optional.of(d), service.getDistributorByUserId(5L));
        verify(distributorRepository).findByUser_IdAndUser_Role_Name(eq(5L), eq("DISTRIBUTOR"));
    }
}
