package com.oktayosman.ticketcenter.repository;

import com.oktayosman.ticketcenter.model.Distributor;
import com.oktayosman.ticketcenter.model.DistributorRating;
import com.oktayosman.ticketcenter.model.Event;
import com.oktayosman.ticketcenter.model.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistributorRatingRepository extends JpaRepository<DistributorRating, Long> {

    Optional<DistributorRating> findByOrganizerAndDistributorAndEvent(Organizer organizer, Distributor distributor, Event event);

    @Query("select avg(dr.ratingValue) from DistributorRating dr where dr.distributor = :distributor")
    Double findAverageByDistributor(@Param("distributor") Distributor distributor);
}

