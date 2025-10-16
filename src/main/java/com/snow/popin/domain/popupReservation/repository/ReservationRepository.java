package com.snow.popin.domain.popupReservation.repository;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends
        JpaRepository<Reservation, Long>,
        ReservationRepositoryCustom {

    /** 특정 사용자의 예약 목록 */
    List<Reservation> findByUser(User currentUser);

    /** 특정 팝업의 예약 목록 */
    List<Reservation> findByPopup(Popup popup);

    /** 특정 팝업과 사용자에 대해 예약 존재 여부 */
    boolean existsByPopupAndUser(Popup popup, User currentUser);

    /** 팝업별 상태별 예약 목록 (시간대별 통계용) */
    List<Reservation> findByPopupAndStatus(Popup popup, ReservationStatus status);
}
