package com.snow.popin.domain.popupReservation.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popupReservation.entity.QReservation;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.snow.popin.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryImpl implements ReservationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QReservation r = QReservation.reservation;

    @Override
    public List<Reservation> findByReservationMinute(String targetMinute) {
        // targetMinute 예: "2025-10-16 14:30"
        LocalDateTime minute = LocalDateTime.parse(
                targetMinute + ":00",
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        return queryFactory
                .selectFrom(r)
                .where(r.reservationDate.between(
                        minute.withSecond(0).withNano(0),
                        minute.withSecond(59).withNano(999_999_999)
                ))
                .fetch();
    }

    @Override
    public boolean existsActiveReservationByPopupAndUser(Popup popup, User user) {
        Long cnt = queryFactory
                .select(r.count())
                .from(r)
                .where(
                        r.popup.eq(popup),
                        r.user.eq(user),
                        r.status.ne(ReservationStatus.CANCELLED)
                )
                .fetchOne();
        return cnt != null && cnt > 0;
    }

    @Override
    public int countByPopupAndTimeRange(Long popupId, LocalDateTime start, LocalDateTime end) {
        Long cnt = queryFactory
                .select(r.count())
                .from(r)
                .where(
                        r.popup.id.eq(popupId),
                        r.reservationDate.goe(start),
                        r.reservationDate.lt(end),
                        r.status.eq(ReservationStatus.RESERVED)
                )
                .fetchOne();
        return cnt == null ? 0 : cnt.intValue();
    }

    @Override
    public long countByPopupAndReservationDateBetween(Popup popup, LocalDateTime startTime, LocalDateTime endTime) {
        Long cnt = queryFactory
                .select(r.count())
                .from(r)
                .where(
                        r.popup.eq(popup),
                        r.reservationDate.goe(startTime),
                        r.reservationDate.lt(endTime),
                        r.status.ne(ReservationStatus.CANCELLED)
                )
                .fetchOne();
        return cnt == null ? 0L : cnt;
    }

    @Override
    public long sumPartySizeByPopupAndReservationDateBetween(Popup popup, LocalDateTime startTime, LocalDateTime endTime) {
        Integer sum = queryFactory
                .select(r.partySize.sum().coalesce(0))
                .from(r)
                .where(
                        r.popup.eq(popup),
                        r.reservationDate.goe(startTime),
                        r.reservationDate.lt(endTime),
                        r.status.ne(ReservationStatus.CANCELLED)
                )
                .fetchOne();
        return sum == null ? 0L : sum.longValue();
    }

    @Override
    public List<Reservation> findByPopupAndReservationDate(Popup popup, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime nextDayStart = startOfDay.plusDays(1);

        return queryFactory
                .selectFrom(r)
                .leftJoin(r.user).fetchJoin()
                .leftJoin(r.popup).fetchJoin()
                .where(
                        r.popup.eq(popup),
                        r.reservationDate.goe(startOfDay),
                        r.reservationDate.lt(nextDayStart)
                )
                .orderBy(r.reservationDate.asc())
                .fetch();
    }

    @Override
    public Long countByPopupAndReservedAtBetweenAndStatus(Popup popup, LocalDateTime start, LocalDateTime end, ReservationStatus status) {
        Long cnt = queryFactory
                .select(r.count())
                .from(r)
                .where(
                        r.popup.eq(popup),
                        r.reservedAt.goe(start),
                        r.reservedAt.lt(end),
                        r.status.eq(status)
                )
                .fetchOne();
        return cnt == null ? 0L : cnt;
    }
}
