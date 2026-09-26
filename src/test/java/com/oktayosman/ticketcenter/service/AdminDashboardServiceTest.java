package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.Organizer;
import com.oktayosman.ticketcenter.model.Role;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.repository.DistributorRepository;
import com.oktayosman.ticketcenter.repository.EventRepository;
import com.oktayosman.ticketcenter.repository.OrganizerRepository;
import com.oktayosman.ticketcenter.repository.RoleRepository;
import com.oktayosman.ticketcenter.repository.TicketSaleRepository;
import com.oktayosman.ticketcenter.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static com.oktayosman.ticketcenter.support.TestData.distributor;
import static com.oktayosman.ticketcenter.support.TestData.organizer;
import static com.oktayosman.ticketcenter.support.TestData.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    private static final double DEFAULT_RATE = 0.10;

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private EntityManager entityManager;
    @Mock private EventRepository eventRepository;
    @Mock private OrganizerRepository organizerRepository;
    @Mock private DistributorRepository distributorRepository;
    @Mock private TicketSaleRepository ticketSaleRepository;

    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        // Built by hand: the constructor also takes the @Value-injected default commission rate.
        service = new AdminDashboardService(userRepository, roleRepository, DEFAULT_RATE, entityManager,
                eventRepository, organizerRepository, distributorRepository, ticketSaleRepository);
    }

    // ---- updateUserRole: validation ----------------------------------------

    @Test
    void updateUserRoleRejectsNullUserId() {
        assertThrows(IllegalArgumentException.class, () -> service.updateUserRole(null, "USER"));
    }

    @Test
    void updateUserRoleRejectsNullOrBlankRole() {
        assertThrows(IllegalArgumentException.class, () -> service.updateUserRole(1L, null));
        assertThrows(IllegalArgumentException.class, () -> service.updateUserRole(1L, "  "));
    }

    @Test
    void updateUserRoleRejectsAdminAndUnknownRoles() {
        assertThrows(IllegalArgumentException.class, () -> service.updateUserRole(1L, "ADMIN"));
        assertThrows(IllegalArgumentException.class, () -> service.updateUserRole(1L, "superuser"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRoleFailsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.updateUserRole(1L, "USER"));
    }

    @Test
    void updateUserRoleFailsWhenRoleRowIsMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "u", "USER")));
        when(roleRepository.findByName("ORGANIZER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.updateUserRole(1L, "ORGANIZER"));
        verify(userRepository, never()).save(any());
    }

    // ---- updateUserRole: behaviour -----------------------------------------

    @Test
    void updateUserRoleNormalisesCaseAndWhitespace() {
        User u = user(1L, "u", "ORGANIZER");
        Role userRole = new Role("USER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(organizerRepository.findById(1L)).thenReturn(Optional.empty());
        when(userRepository.save(u)).thenReturn(u);

        User result = service.updateUserRole(1L, "  user ");

        assertSame(userRole, result.getRole());
    }

    @Test
    void updateUserRoleToSameRoleDoesNotRemoveProfiles() {
        User u = user(1L, "u", "DISTRIBUTOR");
        Query insert = mock(Query.class, RETURNS_SELF);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("DISTRIBUTOR")).thenReturn(Optional.of(new Role("DISTRIBUTOR")));
        when(userRepository.save(u)).thenReturn(u);
        when(entityManager.createNativeQuery(anyString())).thenReturn(insert);

        service.updateUserRole(1L, "DISTRIBUTOR");

        verify(distributorRepository, never()).delete(any());
    }

    @Test
    void updateUserRoleAwayFromDistributorRemovesDistributorProfile() {
        User u = user(1L, "u", "DISTRIBUTOR");
        Distributor profile = distributor(3L, "d");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role("USER")));
        when(distributorRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        when(userRepository.save(u)).thenReturn(u);

        service.updateUserRole(1L, "USER");

        verify(distributorRepository).delete(profile);
        verify(distributorRepository).flush();
    }

    @Test
    void updateUserRoleAwayFromOrganizerRemovesOrganizerProfile() {
        User u = user(1L, "u", "ORGANIZER");
        Organizer profile = organizer(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role("USER")));
        when(organizerRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(userRepository.save(u)).thenReturn(u);

        service.updateUserRole(1L, "USER");

        verify(organizerRepository).delete(profile);
        verify(organizerRepository).flush();
    }

    @Test
    void updateUserRoleExplainsWhenDistributorProfileIsStillReferenced() {
        User u = user(1L, "u", "DISTRIBUTOR");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role("USER")));
        Distributor profile = distributor(3L, "d");
        when(distributorRepository.findByUser_Id(1L)).thenReturn(Optional.of(profile));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk")).when(distributorRepository).flush();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.updateUserRole(1L, "USER"));

        assertTrue(ex.getMessage().contains("ticket sales"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRoleExplainsWhenOrganizerProfileIsStillReferenced() {
        User u = user(1L, "u", "ORGANIZER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role("USER")));
        when(organizerRepository.findById(1L)).thenReturn(Optional.of(organizer(1L)));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk")).when(organizerRepository).flush();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.updateUserRole(1L, "USER"));

        assertTrue(ex.getMessage().contains("events"));
    }

    @Test
    void updateUserRoleToOrganizerCreatesProfileWhenMissing() {
        User u = user(1L, "u", "USER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("ORGANIZER")).thenReturn(Optional.of(new Role("ORGANIZER")));
        when(userRepository.save(u)).thenReturn(u);
        when(organizerRepository.existsById(1L)).thenReturn(false);

        service.updateUserRole(1L, "ORGANIZER");

        ArgumentCaptor<Organizer> captor = ArgumentCaptor.forClass(Organizer.class);
        verify(organizerRepository).save(captor.capture());
        assertSame(u, captor.getValue().getUser());
        assertEquals("", captor.getValue().getOrganizationName());
        assertEquals(0.0, captor.getValue().getCommission());
    }

    @Test
    void updateUserRoleToOrganizerKeepsExistingProfile() {
        User u = user(1L, "u", "USER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("ORGANIZER")).thenReturn(Optional.of(new Role("ORGANIZER")));
        when(userRepository.save(u)).thenReturn(u);
        when(organizerRepository.existsById(1L)).thenReturn(true);

        service.updateUserRole(1L, "ORGANIZER");

        verify(organizerRepository, never()).save(any());
    }

    @Test
    void updateUserRoleToDistributorInsertsProfileWithDefaultCommission() {
        User u = user(1L, "u", "USER");
        Query insert = mock(Query.class, RETURNS_SELF);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(roleRepository.findByName("DISTRIBUTOR")).thenReturn(Optional.of(new Role("DISTRIBUTOR")));
        when(userRepository.save(u)).thenReturn(u);
        when(entityManager.createNativeQuery(anyString())).thenReturn(insert);

        service.updateUserRole(1L, "DISTRIBUTOR");

        verify(insert).setParameter("userId", 1L);
        verify(insert).setParameter("commissionRate", DEFAULT_RATE);
        verify(insert).executeUpdate();
    }

    // ---- createUserAccount -------------------------------------------------

    @Test
    void createUserAccountRejectsTakenUsername() {
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(user(1L, "taken", "USER")));

        assertThrows(IllegalArgumentException.class, () -> service.createUserAccount(
                "A", "B", "taken", "a@b.com", "pw", "USER", null, null, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserAccountRejectsRegisteredEmail() {
        when(userRepository.findByUsername("fresh")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user(1L, "x", "USER")));

        assertThrows(IllegalArgumentException.class, () -> service.createUserAccount(
                "A", "B", "fresh", "a@b.com", "pw", "USER", null, null, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserAccountFailsWhenRoleRowIsMissing() {
        when(userRepository.findByUsername("fresh")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.createUserAccount(
                "A", "B", "fresh", "a@b.com", "pw", " user ", null, null, null));
    }

    private void stubNewAccount(String roleName) {
        when(userRepository.findByUsername("fresh")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(new Role(roleName)));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createUserAccountForPlainUserCreatesNoProfile() {
        stubNewAccount("USER");

        User created = service.createUserAccount("A", "B", "fresh", "a@b.com", "pw", "USER", null, null, null);

        assertEquals("USER", created.getRole().getName());
        verify(organizerRepository, never()).save(any());
        verify(distributorRepository, never()).save(any());
    }

    @Test
    void createUserAccountHashesPassword() {
        stubNewAccount("USER");

        User created = service.createUserAccount("A", "B", "fresh", "a@b.com", "pw", "USER", null, null, null);

        assertTrue(created.verifyPassword("pw"));
    }

    @Test
    void createUserAccountForOrganizerUsesGivenDetails() {
        stubNewAccount("ORGANIZER");

        service.createUserAccount("A", "B", "fresh", "a@b.com", "pw", "organizer", "Acme", 12.5, null);

        ArgumentCaptor<Organizer> captor = ArgumentCaptor.forClass(Organizer.class);
        verify(organizerRepository).save(captor.capture());
        assertEquals("Acme", captor.getValue().getOrganizationName());
        assertEquals(12.5, captor.getValue().getCommission());
    }

    @Test
    void createUserAccountForOrganizerDefaultsMissingDetails() {
        stubNewAccount("ORGANIZER");

        service.createUserAccount("A", "B", "fresh", "a@b.com", "pw", "ORGANIZER", null, null, null);

        ArgumentCaptor<Organizer> captor = ArgumentCaptor.forClass(Organizer.class);
        verify(organizerRepository).save(captor.capture());
        assertEquals("", captor.getValue().getOrganizationName());
        assertEquals(0.0, captor.getValue().getCommission());
    }

    @Test
    void createUserAccountForDistributorUsesExplicitCommissionRate() {
        stubNewAccount("DISTRIBUTOR");

        service.createUserAccount("A", "B", "fresh", "a@b.com", "pw", "DISTRIBUTOR", null, null, 0.25);

        ArgumentCaptor<Distributor> captor = ArgumentCaptor.forClass(Distributor.class);
        verify(distributorRepository).save(captor.capture());
        assertEquals(0.25, captor.getValue().getCommissionRate());
    }

    @Test
    void createUserAccountForDistributorFallsBackToDefaultCommissionRate() {
        stubNewAccount("DISTRIBUTOR");

        service.createUserAccount("A", "B", "fresh", "a@b.com", "pw", "DISTRIBUTOR", null, null, null);

        ArgumentCaptor<Distributor> captor = ArgumentCaptor.forClass(Distributor.class);
        verify(distributorRepository).save(captor.capture());
        assertEquals(DEFAULT_RATE, captor.getValue().getCommissionRate());
    }

    // ---- profile updates ---------------------------------------------------

    @Test
    void updateDistributorCommissionSetsRateAndSaves() {
        Distributor d = distributor(3L, "d");
        when(distributorRepository.findById(3L)).thenReturn(Optional.of(d));

        service.updateDistributorCommission(3L, 0.3);

        assertEquals(0.3, d.getCommissionRate());
        verify(distributorRepository).save(d);
    }

    @Test
    void updateDistributorCommissionFailsForUnknownDistributor() {
        when(distributorRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.updateDistributorCommission(3L, 0.3));
    }

    @Test
    void updateOrganizerProfileOnlyChangesProvidedFields() {
        Organizer o = organizer(7L);
        when(organizerRepository.findById(7L)).thenReturn(Optional.of(o));

        service.updateOrganizerProfile(7L, null, 9.0);
        assertEquals("Org", o.getOrganizationName());
        assertEquals(9.0, o.getCommission());

        service.updateOrganizerProfile(7L, "NewName", null);
        assertEquals("NewName", o.getOrganizationName());
        assertEquals(9.0, o.getCommission());
    }

    @Test
    void updateOrganizerProfileFailsForUnknownOrganizer() {
        when(organizerRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.updateOrganizerProfile(7L, "x", 1.0));
    }

    // ---- statistics --------------------------------------------------------

    @Test
    void statisticsDefaultToZeroWhenRepositoryReturnsNull() {
        when(ticketSaleRepository.getTotalTicketsSold()).thenReturn(null);
        when(ticketSaleRepository.getTotalRevenue()).thenReturn(null);
        Distributor d = distributor(3L, "d");
        when(ticketSaleRepository.getTicketsSoldForDistributor(d)).thenReturn(null);
        when(ticketSaleRepository.getRevenueForDistributor(d)).thenReturn(null);

        assertEquals(0L, service.getTotalTicketsSold());
        assertEquals(BigDecimal.ZERO, service.getTotalRevenue());
        assertEquals(0L, service.getTicketsSoldForDistributor(d));
        assertEquals(BigDecimal.ZERO, service.getRevenueForDistributor(d));
    }

    @Test
    void statisticsPassThroughRepositoryValues() {
        when(ticketSaleRepository.getTotalTicketsSold()).thenReturn(42L);
        when(ticketSaleRepository.getTotalRevenue()).thenReturn(new BigDecimal("99.90"));
        when(userRepository.count()).thenReturn(5L);
        when(eventRepository.count()).thenReturn(3L);

        assertEquals(42L, service.getTotalTicketsSold());
        assertEquals(new BigDecimal("99.90"), service.getTotalRevenue());
        assertEquals(5, service.getTotalUsers());
        assertEquals(3, service.getTotalEvents());
    }

    @Test
    void assignableRolesDoNotIncludeAdmin() {
        assertEquals(java.util.List.of("USER", "ORGANIZER", "DISTRIBUTOR"), service.getAssignableRoleNames());
    }
}
