package com.oktayosman.ticketcenter.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeatTypeTest {

    @Test
    void availableSeatsIsTotalMinusSold() {
        // Arrange
        SeatType seatType = new SeatType();
        seatType.setTotalSeats(100);
        seatType.setSoldSeats(30);

        // Act
        int available = seatType.getAvailableSeats();

        // Assert
        assertEquals(70, available);
    }

    @Test
    void availableSeatsIsUnlimitedWhenTotalSeatsIsNull() {
        SeatType seatType = new SeatType();
        seatType.setTotalSeats(null);
        seatType.setSoldSeats(30);

        assertEquals(Integer.MAX_VALUE, seatType.getAvailableSeats());
    }

    @Test
    void availableSeatsIsZeroWhenSoldOutExactly() {
        SeatType seatType = new SeatType();
        seatType.setTotalSeats(50);
        seatType.setSoldSeats(50);

        assertEquals(0, seatType.getAvailableSeats());
    }
}
