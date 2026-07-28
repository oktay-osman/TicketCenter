package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Role;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.repository.RoleRepository;
import com.oktayosman.ticketcenter.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

@Service
public class AdminDashboardService {

    private static final String DISTRIBUTOR_ROLE_NAME = "DISTRIBUTOR";
    private static final List<String> ASSIGNABLE_ROLE_NAMES = List.of("USER", "ORGANIZER", "DISTRIBUTOR");
    private static final Set<String> ASSIGNABLE_ROLE_NAME_SET = Set.copyOf(ASSIGNABLE_ROLE_NAMES);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EntityManager entityManager;
    private final double defaultDistributorCommissionRate;

    @Autowired
    public AdminDashboardService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 @Value("${app.distributor.default-commission-rate:0.10}") double defaultDistributorCommissionRate,
                                 EntityManager entityManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.defaultDistributorCommissionRate = defaultDistributorCommissionRate;
        this.entityManager = entityManager;
    }

    public int getTotalUsers() {
        return (int) userRepository.count();
    }

    public int getTotalEvents() {
        return 1; // Placeholder until EventRepository is implemented
    }

    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"));
    }

    public List<String> getAssignableRoleNames() {
        return ASSIGNABLE_ROLE_NAMES;
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

        user.setRole(role);
        User savedUser = userRepository.save(user);

        if (DISTRIBUTOR_ROLE_NAME.equals(normalizedRoleName)) {
            ensureDistributorRecord(savedUser.getId());
        }

        return savedUser;
    }

    private void ensureDistributorRecord(Long userId) {
        entityManager.createNativeQuery(
                        "INSERT INTO distributors (user_id, commission_rate, rating) VALUES (:userId, :commissionRate, NULL) " +
                                "ON CONFLICT (user_id) DO NOTHING")
                .setParameter("userId", userId)
                .setParameter("commissionRate", defaultDistributorCommissionRate)
                .executeUpdate();
    }

}
