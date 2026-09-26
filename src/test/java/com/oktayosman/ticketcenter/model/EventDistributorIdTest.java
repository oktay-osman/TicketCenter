package com.oktayosman.ticketcenter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EventDistributorIdTest {

    @Test
    void sameIdsAreEqual() {
        //Arrange
        EventDistributorId a = new EventDistributorId(1L, 100L);
        EventDistributorId b = new EventDistributorId(1L, 100L);

        //Act
        boolean equalValues = a.equals(b);

        //Assert
        assertTrue(equalValues);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentDistributorIdIsNotEqual() {
        //Arrange
        EventDistributorId a = new EventDistributorId(1L, 100L);
        EventDistributorId b = new EventDistributorId(1L, 101L);

        //Act + Assert
        assertNotEquals(a, b);
    }

    @Test
    void notEqualToNull() {
        EventDistributorId a = new EventDistributorId();
        assertFalse(a.equals(null));
    }

    @Test
    void equalToItself() {
        EventDistributorId a = new EventDistributorId();
        assertTrue(a.equals(a));
    }



}
