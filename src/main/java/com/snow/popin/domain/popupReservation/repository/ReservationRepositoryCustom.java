package com.snow.popin.domain.popupReservation.repository;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.snow.popin.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepositoryCustom {

    /** 특정 “분” 범위 내 예약 조회 (yyyy-MM-dd HH:mm) */
    List<Reservation> findByReservationMinute(String targetMinute);

    /** 취소되지 않은 활성 예약 존재 여부 */
    boolean existsActiveReservationByPopupAndUser(Popup popup, User user);

    /** 특정 팝업의 특정 시간 범위 예약 수 (RESERVED) */
    int countByPopupAndTimeRange(Long popupId, LocalDateTime start, LocalDateTime end);

    /** 특정 팝업의 특정 시간대 예약 개수 (CANCELLED 제외) */
    long countByPopupAndReservationDateBetween(Popup popup, LocalDateTime startTime, LocalDateTime endTime);

    /** 특정 팝업의 특정 시간대 예약 인원 합계 (CANCELLED 제외) */
    long sumPartySizeByPopupAndReservationDateBetween(Popup popup, LocalDateTime startTime, LocalDateTime endTime);

    /** 특정 팝업의 특정 ‘날짜’ 예약 목록 (당일 00:00~24:00) */
    List<Reservation> findByPopupAndReservationDate(Popup popup, LocalDateTime date);

    /** 특정 팝업의 특정 기간 내 상태별 예약 개수 (reservedAt 기준) */
    Long countByPopupAndReservedAtBetweenAndStatus(Popup popup, LocalDateTime start, LocalDateTime end, ReservationStatus status);
}
