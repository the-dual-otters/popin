package com.snow.popin.domain.spacereservation.repository;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import com.snow.popin.domain.spacereservation.entity.ReservationStatus;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceReservationRepository extends
        JpaRepository<SpaceReservation, Long>,
        SpaceReservationRepositoryCustom {

    // 특정 사용자가 요청한 예약 목록 (HOST)
    List<SpaceReservation> findByHostAndIsHiddenFalseOrderByCreatedAtDesc(User host);

    // 특정 사용자의 특정 예약 조회 (HOST)
    Optional<SpaceReservation> findByIdAndHostAndIsHiddenFalse(Long id, User host);

}
