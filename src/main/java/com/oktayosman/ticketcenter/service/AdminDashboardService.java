package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.Role;
import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.repository.RoleRepository;
import com.oktayosman.ticketcenter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

@Service
public class AdminDashboardService {

    private static final List<String> ASSIGNABLE_ROLE_NAMES = List.of("USER", "ORGANIZER", "DISTRIBUTOR");
    private static final Set<String> ASSIGNABLE_ROLE_NAME_SET = Set.copyOf(ASSIGNABLE_ROLE_NAMES);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public AdminDashboardService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
        return userRepository.save(user);
    }

}
