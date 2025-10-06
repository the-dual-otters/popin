package com.snow.popin.domain.popupReservation.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.snow.popin.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.snow.popin.domain.popupReservation.entity.QReservation.reservation;

@Repository
@RequiredArgsConstructor
public class ReservationQueryDslRepository {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    /**
     * 특정 시간 범위 내의 예약 조회 (예: 예약 30분 전 확인용)
     */
    public List<Reservation> findByReservationMinute(String targetMinute) {
        LocalDateTime targetDateTime = LocalDateTime.parse(targetMinute + ":00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return queryFactory
                .selectFrom(reservation)
                .where(reservation.reservationDate.between(
                        targetDateTime.withSecond(0),
                        targetDateTime.withSecond(59).withNano(999999999)
                ))
                .fetch();
    }
    /**
     * 취소되지 않은 활성 예약 존재 여부 확인
     */
    public boolean existsActiveReservationByPopupAndUser(Popup popup, User user) {
        Long count = queryFactory
                .select(reservation.count())
                .from(reservation)
                .where(
                        reservation.popup.eq(popup)
                                .and(reservation.user.eq(user))
                                .and(reservation.status.ne(ReservationStatus.CANCELLED))
                )
                .fetchOne();

        return count != null && count > 0;
    }

    /**
     * 특정 팝업의 특정 시간 범위 예약 수 조회
     */
    public int countByPopupAndTimeRange(Long popupId, LocalDateTime start, LocalDateTime end) {
        Long count = queryFactory
                .select(reservation.count())
                .from(reservation)
                .where(
                        reservation.popup.id.eq(popupId)
                                .and(reservation.reservationDate.goe(start))
                                .and(reservation.reservationDate.lt(end))
                                .and(reservation.status.eq(ReservationStatus.RESERVED))
                )
                .fetchOne();
        return count != null ? count.intValue() : 0;
    }

    /**
     * 특정 팝업의 특정 시간대 예약 개수 조회
     */
    public long countByPopupAndReservationDateBetween(Popup popup, LocalDateTime startTime, LocalDateTime endTime) {
        Long count = queryFactory
                .select(reservation.count())
                .from(reservation)
                .where(
                        reservation.popup.eq(popup)
                                .and(reservation.reservationDate.goe(startTime))
                                .and(reservation.reservationDate.lt(endTime))
                                .and(reservation.status.ne(ReservationStatus.CANCELLED))
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    /**
     * 특정 팝업의 특정 시간대 예약 인원 수 조회 (파티 사이즈 합계)
     */
    public long sumPartySizeByPopupAndReservationDateBetween(Popup popup, LocalDateTime startTime, LocalDateTime endTime) {
        Integer sum = queryFactory
                .select(reservation.partySize.sum().coalesce(0))
                .from(reservation)
                .where(
                        reservation.popup.eq(popup)
                                .and(reservation.reservationDate.goe(startTime))
                                .and(reservation.reservationDate.lt(endTime))
                                .and(reservation.status.ne(ReservationStatus.CANCELLED))
                )
                .fetchOne();
        return sum != null ? sum.longValue() : 0L;
    }

    /**
     * 특정 팝업의 특정 날짜 예약 목록 조회
     */
    public List<Reservation> findByPopupAndReservationDate(Popup popup, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime nextDayStart = startOfDay.plusDays(1);

        return queryFactory
                .selectFrom(reservation)
                .leftJoin(reservation.user).fetchJoin()
                .leftJoin(reservation.popup).fetchJoin()
                .where(
                        reservation.popup.eq(popup)
                                .and(reservation.reservationDate.goe(startOfDay))
                                .and(reservation.reservationDate.lt(nextDayStart))
                )
                .orderBy(reservation.reservationDate.asc())
                .fetch();
    }

    /**
     * 특정 팝업의 특정 기간 내 예약 상태별 개수 조회
     */
    public Long countByPopupAndReservedAtBetweenAndStatus(Popup popup, LocalDateTime start,
                                                          LocalDateTime end, ReservationStatus status) {
        Long count = queryFactory
                .select(reservation.count())
                .from(reservation)
                .where(
                        reservation.popup.eq(popup)
                                .and(reservation.reservedAt.goe(start))
                                .and(reservation.reservedAt.lt(end))
                                .and(reservation.status.eq(status))
                )
                .fetchOne();
        return count != null ? count : 0L;
    }
}