package com.oktayosman.ticketcenter.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void verifyPasswordSucceedsForTheCorrectPassword() {
        User user = new User("Jane", "Doe", "jane@example.com", "janedoe", "correct-horse", null);

        assertTrue(user.verifyPassword("correct-horse"));
    }

    @Test
    void verifyPasswordFailsForTheWrongPassword() {
        User user = new User("Jane", "Doe", "jane@example.com", "janedoe", "correct-horse", null);

        assertFalse(user.verifyPassword("wrong-password"));
    }

    @Test
    void passwordIsStoredHashedNotInPlainText() {
        User user = new User("Jane", "Doe", "jane@example.com", "janedoe", "correct-horse", null);

        assertNotEquals("correct-horse", user.getPassword());
    }

    @Test
    void settingABlankPasswordThrows() {
        User user = new User();

        assertThrows(IllegalArgumentException.class, () -> user.setPassword("   "));
    }
}
