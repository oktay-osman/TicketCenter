package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Distributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, Long> {
	Optional<Distributor> findByUser_IdAndUser_Role_Name(Long userId, String roleName);
}
