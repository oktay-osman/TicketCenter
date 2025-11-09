package com.oktayosman.ticketcenter.service;

import com.oktayosman.ticketcenter.model.User;
import com.oktayosman.ticketcenter.model.Role;
import com.oktayosman.ticketcenter.repository.UserRepository;
import com.oktayosman.ticketcenter.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final String DEFAULT_ROLE_NAME = "USER";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public User authenticate(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> user.verifyPassword(password))
                .orElse(null);
    }

    public boolean registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return false;
        }

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException("Default role not found: " + DEFAULT_ROLE_NAME));
        user.setRole(defaultRole);
        userRepository.save(user);
        return true;
    }
}