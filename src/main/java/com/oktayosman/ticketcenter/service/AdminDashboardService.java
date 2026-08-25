package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.*;
import com.oktayosman.ticketcenter.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class AdminDashboardService {

    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";
    private static final String ORGANIZER_ROLE_NAME = "ORGANIZER";
    private static final List<String> ASSIGNABLE_ROLE_NAMES = List.of("USER", "ORGANIZER", "DISTRIBUTOR");
    private static final Set<String> ASSIGNABLE_ROLE_NAME_SET = Set.copyOf(ASSIGNABLE_ROLE_NAMES);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EntityManager entityManager;
    private final EventRepository eventRepository;
    private final OrganizerRepository organizerRepository;
    private final DistributorRepository distributorRepository;
    private final TicketSaleRepository ticketSaleRepository;
    private final double defaultDistributorCommissionRate;

    @Autowired
    public AdminDashboardService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 @Value("${app.distributor.default-commission-rate:0.10}") double defaultDistributorCommissionRate,
                                 EntityManager entityManager,
                                 EventRepository eventRepository,
                                 OrganizerRepository organizerRepository,
                                 DistributorRepository distributorRepository,
                                 TicketSaleRepository ticketSaleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.defaultDistributorCommissionRate = defaultDistributorCommissionRate;
        this.entityManager = entityManager;
        this.eventRepository = eventRepository;
        this.organizerRepository = organizerRepository;
        this.distributorRepository = distributorRepository;
        this.ticketSaleRepository = ticketSaleRepository;
    }

    public int getTotalUsers() {
        return (int) userRepository.count();
    }

    public int getTotalEvents() {
        return (int) eventRepository.count();
    }

    public long getTotalTicketsSold() {
        Long count = ticketSaleRepository.getTotalTicketsSold();
        return count != null ? count : 0L;
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = ticketSaleRepository.getTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"));
    }

    public List<String> getAssignableRoleNames() {
        return ASSIGNABLE_ROLE_NAMES;
    }

    public List<Organizer> getAllOrganizers() {
        return organizerRepository.findAll();
    }

    public List<Distributor> getAllDistributors() {
        return distributorRepository.findAll();
    }

    @Transactional
    public User updateUserRole(Long userId, String roleName) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        String normalizedRoleName = roleName.trim().toUpperCase();
        if (!ASSIGNABLE_ROLE_NAME_SET.contains(normalizedRoleName)) {
            throw new IllegalArgumentException("ADMIN role cannot be assigned from the admin users page");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + normalizedRoleName));

        String oldRoleName = user.getRole() != null ? user.getRole().getName() : null;

        // Clean up the old profile record when switching away from a role
        if (!normalizedRoleName.equals(oldRoleName)) {
            if (DISTRIBUTOR_ROLE_NAME.equals(oldRoleName)) {
                removeDistributorProfile(userId);
            } else if (ORGANIZER_ROLE_NAME.equals(oldRoleName)) {
                removeOrganizerProfile(userId);
            }
        }

        user.setRole(role);
        User savedUser = userRepository.save(user);

        if (DISTRIBUTOR_ROLE_NAME.equals(normalizedRoleName)) {
            ensureDistributorRecord(savedUser.getId());
        } else if (ORGANIZER_ROLE_NAME.equals(normalizedRoleName)) {
            ensureOrganizerRecord(savedUser);
        }

        return savedUser;
    }

    @Transactional
    public User createUserAccount(String firstName, String lastName, String username, String email,
                                  String password, String roleName,
                                  String organizationName, Double commission, Double commissionRate) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered");
        }

        String normalizedRoleName = roleName.trim().toUpperCase();
        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + normalizedRoleName));

        User user = new User(firstName, lastName, email, username, password, role);
        User savedUser = userRepository.save(user);
        entityManager.flush();

        if (ORGANIZER_ROLE_NAME.equals(normalizedRoleName)) {
            Organizer organizer = new Organizer(savedUser,
                    organizationName != null ? organizationName : "",
                    commission != null ? commission : 0.0);
            organizerRepository.save(organizer);
        } else if (DISTRIBUTOR_ROLE_NAME.equals(normalizedRoleName)) {
            double rate = commissionRate != null ? commissionRate : defaultDistributorCommissionRate;
            Distributor distributor = new Distributor(savedUser, rate);
            distributorRepository.save(distributor);
        }

        return savedUser;
    }

    @Transactional
    public void updateDistributorCommission(Long distributorId, double commissionRate) {
        Distributor distributor = distributorRepository.findById(distributorId)
                .orElseThrow(() -> new IllegalStateException("Distributor not found: " + distributorId));
        distributor.setCommissionRate(commissionRate);
        distributorRepository.save(distributor);
    }

    @Transactional
    public void updateOrganizerProfile(Long organizerId, String organizationName, Double commission) {
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalStateException("Organizer not found: " + organizerId));
        if (organizationName != null) {
            organizer.setOrganizationName(organizationName);
        }
        if (commission != null) {
            organizer.setCommission(commission);
        }
        organizerRepository.save(organizer);
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public long getTicketsSoldForDistributor(Distributor distributor) {
        Long count = ticketSaleRepository.getTicketsSoldForDistributor(distributor);
        return count != null ? count : 0L;
    }

    public BigDecimal getRevenueForDistributor(Distributor distributor) {
        BigDecimal revenue = ticketSaleRepository.getRevenueForDistributor(distributor);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    private void removeDistributorProfile(Long userId) {
        distributorRepository.findByUser_Id(userId).ifPresent(distributor -> {
            try {
                distributorRepository.delete(distributor);
                distributorRepository.flush();
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException(
                        "Cannot change role: this distributor has existing ticket sales or event assignments. " +
                        "Remove those records first.");
            }
        });
    }

    private void removeOrganizerProfile(Long userId) {
        organizerRepository.findById(userId).ifPresent(organizer -> {
            try {
                organizerRepository.delete(organizer);
                organizerRepository.flush();
            } catch (DataIntegrityViolationException e) {
                throw new IllegalStateException(
                        "Cannot change role: this organizer has existing events or ratings. " +
                        "Remove those records first.");
            }
        });
    }

    private void ensureDistributorRecord(Long userId) {
        entityManager.createNativeQuery(
                        "INSERT INTO distributors (user_id, commission_rate, rating) VALUES (:userId, :commissionRate, NULL) " +
                                "ON CONFLICT (user_id) DO NOTHING")
                .setParameter("userId", userId)
                .setParameter("commissionRate", defaultDistributorCommissionRate)
                .executeUpdate();
    }

    @Transactional
    private void ensureOrganizerRecord(User user) {
        if (!organizerRepository.existsById(user.getId())) {
            Organizer organizer = new Organizer(user, "", 0.0);
            organizerRepository.save(organizer);
        }
    }
}
