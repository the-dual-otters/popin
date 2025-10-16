package com.snow.popin.domain.spacereservation.repository;

import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpaceReservationRepositoryCustom {

    /** 특정 공간 소유자(PROVIDER)에게 온 예약 목록 (최신순) */
    List<SpaceReservation> findBySpaceOwnerOrderByCreatedAtDesc(User owner);

    /** 특정 공간 소유자의 특정 예약 조회 */
    Optional<SpaceReservation> findByIdAndSpaceOwner(Long id, User owner);

    /** 기간 겹침(Overlap) 예약 카운트 (ACCEPTED & not hidden) */
    long countOverlappingReservations(Space space, LocalDate startDate, LocalDate endDate);
}
