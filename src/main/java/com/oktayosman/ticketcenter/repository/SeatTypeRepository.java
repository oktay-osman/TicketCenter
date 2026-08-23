package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.SeatCategory;
import com.oktayosman.ticketcenter.model.SeatType;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatTypeRepository extends JpaRepository<SeatType, Long> {

    Optional<SeatType> findByEventAndSeatCategory(Event event, SeatCategory seatCategory);

    List<SeatType> findByEvent(Event event);

    /**
     * Reads a seat type with a row-level write lock (SELECT ... FOR UPDATE) so that
     * concurrent sales of the same seat type serialize instead of overselling.
     * Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatType s WHERE s.id = :id")
    Optional<SeatType> findByIdForUpdate(@Param("id") Long id);
}
