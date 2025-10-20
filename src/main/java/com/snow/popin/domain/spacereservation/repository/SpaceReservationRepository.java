package com.snow.popin.domain.spacereservation.repository;

import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpaceReservationRepository extends JpaRepository<SpaceReservation, Long>,
        SpaceReservationRepositoryCustom {
}