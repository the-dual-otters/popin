package com.snow.popin.domain.spacereservation.repository;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import com.snow.popin.domain.spacereservation.entity.ReservationStatus;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceReservationRepository extends JpaRepository<SpaceReservation, Long> {

    // 특정 사용자가 요청한 예약 목록 (HOST)
    List<SpaceReservation> findByHostAndIsHiddenFalseOrderByCreatedAtDesc(User host);

    // 특정 공간 소유자에게 온 예약 목록 (PROVIDER)
    @Query("SELECT sr FROM SpaceReservation sr " +
            "WHERE sr.space.owner = :owner AND sr.isHidden = false " +
            "ORDER BY sr.createdAt DESC")
    List<SpaceReservation> findBySpaceOwnerOrderByCreatedAtDesc(@Param("owner") User owner);

    // 특정 사용자의 특정 예약 조회
    Optional<SpaceReservation> findByIdAndHostAndIsHiddenFalse(Long id, User host);

    // 특정 공간 소유자의 특정 예약 조회
    @Query("SELECT sr FROM SpaceReservation sr WHERE sr.id = :id AND sr.space.owner = :owner AND sr.isHidden = false")
    Optional<SpaceReservation> findByIdAndSpaceOwner(@Param("id") Long id, @Param("owner") User owner);

    // 날짜 중복 체크용 - 특정 공간의 특정 기간에 승인된 예약이 있는지 확인
    @Query("SELECT COUNT(sr) FROM SpaceReservation sr WHERE sr.space = :space " +
            "AND sr.status = 'ACCEPTED' AND sr.isHidden = false " +
            "AND ((sr.startDate <= :endDate AND sr.endDate >= :startDate))")
    long countOverlappingReservations(@Param("space") Space space,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    // 특정 공간의 모든 예약 목록
    List<SpaceReservation> findBySpaceAndIsHiddenFalseOrderByStartDateDesc(Space space);

    // 특정 상태의 예약 목록
    List<SpaceReservation> findByStatusAndIsHiddenFalseOrderByCreatedAtDesc(ReservationStatus status);
}
