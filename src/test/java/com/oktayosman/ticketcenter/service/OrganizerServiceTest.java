package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.oktayosman.ticketcenter.support.TestData.organizer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizerServiceTest {

    @Mock
    private OrganizerRepository organizerRepository;

    @InjectMocks
    private OrganizerService service;

    @Test
    void getOrganizerByUserIdReturnsEmptyForNullIdWithoutQuerying() {
        assertTrue(service.getOrganizerByUserId(null).isEmpty());
        verify(organizerRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getOrganizerByUserIdDelegatesToRepository() {
        Organizer o = organizer(7L);
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(o));

        assertSame(o, service.getOrganizerByUserId(7L).orElseThrow());
    }

    @Test
    void updateOrganizerProfileTrimsName() {
        Organizer o = organizer(7L);
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(o));
        when(organizerRepository.save(o)).thenReturn(o);

        Organizer result = service.updateOrganizerProfile(7L, "  Acme Events ");

        assertEquals("Acme Events", result.getOrganizationName());
    }

    @Test
    void updateOrganizerProfileStoresEmptyStringForNullName() {
        Organizer o = organizer(7L);
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(o));
        when(organizerRepository.save(o)).thenReturn(o);

        assertEquals("", service.updateOrganizerProfile(7L, null).getOrganizationName());
    }

    @Test
    void updateOrganizerProfileFailsWhenProfileIsMissing() {
        when(organizerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.updateOrganizerProfile(7L, "x"));
    }
}
