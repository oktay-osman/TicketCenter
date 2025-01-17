package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {
    User getUserByUsername(String username);
}
