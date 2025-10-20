package com.snow.popin.domain.spacereservation.repository;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import com.snow.popin.domain.spacereservation.entity.ReservationStatus;
import com.snow.popin.domain.user.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpaceReservationRepositoryCustom {
    List<SpaceReservation> findByHostAndIsHiddenFalseOrderByCreatedAtDesc(User host);
    List<SpaceReservation> findBySpaceOwnerOrderByCreatedAtDesc(User owner);
    Optional<SpaceReservation> findByIdAndHostAndIsHiddenFalse(Long id, User host);
    Optional<SpaceReservation> findByIdAndSpaceOwner(Long id, User owner);

    long countOverlappingReservations(Space space, LocalDate startDate, LocalDate endDate);
}