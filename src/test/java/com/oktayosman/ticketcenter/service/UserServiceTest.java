package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Role;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.repository.RoleRepository;
import com.oktayosman.ticketcenter.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUserSucceedsAndAssignsDefaultRoleWhenUsernameIsNew() {
        // Arrange
        User newUser = new User("Jane", "Doe", "jane@example.com", "janedoe", "password123", null);
        Role defaultRole = new Role("USER");

        when(userRepository.findByUsername("janedoe")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));

        // Act
        boolean result = userService.registerUser(newUser);

        // Assert
        assertTrue(result);
        assertTrue(newUser.getRole() == defaultRole);
        verify(userRepository).save(newUser);
    }

    @Test
    void registerUserFailsWhenUsernameAlreadyTaken() {
        User existingUser = new User("Jane", "Doe", "jane@example.com", "janedoe", "password123", null);

        when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(existingUser));

        assertFalse(userService.registerUser(existingUser));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserThrowsWhenDefaultRoleMissing() {
        User newUser = new User("Jane", "Doe", "jane@example.com", "janedoe", "password123", null);

        when(userRepository.findByUsername("janedoe")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> userService.registerUser(newUser));
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateReturnsUserWhenPasswordIsCorrect() {
        User realUser = new User("Jane", "Doe", "jane@example.com", "janedoe", "password123", null);

        when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(realUser));

        assertSame(realUser, userService.authenticate("janedoe", "password123"));
    }

    @Test
    void authenticateReturnsNullWhenPasswordIsWrong() {
        User realUser = new User("Jane", "Doe", "jane@example.com", "janedoe", "password123", null);

        when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(realUser));

        assertNull(userService.authenticate("janedoe", "wrong-password"));
    }

    @Test
    void authenticateReturnsNullWhenUserDoesNotExist() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertNull(userService.authenticate("ghost", "password123"));
    }

    @Test
    void authenticateReturnsNullWhenPasswordIsNull() {
        User realUser = new User("Jane", "Doe", "jane@example.com", "janedoe", "password123", null);
        when(userRepository.findByUsername("janedoe")).thenReturn(Optional.of(realUser));

        assertNull(userService.authenticate("janedoe", null));
    }
}
