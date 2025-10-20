package com.snow.popin.domain.spacereservation.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.space.entity.QSpace;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.spacereservation.entity.QSpaceReservation;
import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import com.snow.popin.domain.spacereservation.entity.ReservationStatus;
import com.snow.popin.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpaceReservationRepositoryImpl implements SpaceReservationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QSpaceReservation sr = QSpaceReservation.spaceReservation;

    @Override
    public List<SpaceReservation> findByHostAndIsHiddenFalseOrderByCreatedAtDesc(User host) {
        return queryFactory
                .selectFrom(sr)
                .where(
                        sr.host.eq(host),
                        sr.isHidden.isFalse()
                )
                .orderBy(sr.createdAt.desc())
                .fetch();
    }

    @Override
    public List<SpaceReservation> findBySpaceOwnerOrderByCreatedAtDesc(User owner) {
        return queryFactory
                .selectFrom(sr)
                .where(
                        sr.space.owner.eq(owner),
                        sr.isHidden.isFalse()
                )
                .orderBy(sr.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<SpaceReservation> findByIdAndHostAndIsHiddenFalse(Long id, User host) {
        SpaceReservation result = queryFactory
                .selectFrom(sr)
                .where(
                        sr.id.eq(id),
                        sr.host.eq(host),
                        sr.isHidden.isFalse()
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<SpaceReservation> findByIdAndSpaceOwner(Long id, User owner) {
        SpaceReservation result = queryFactory
                .selectFrom(sr)
                .where(
                        sr.id.eq(id),
                        sr.space.owner.eq(owner),
                        sr.isHidden.isFalse()
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public long countOverlappingReservations(Space space, LocalDate startDate, LocalDate endDate) {
        Long count = queryFactory
                .select(sr.count())
                .from(sr)
                .where(
                        sr.space.eq(space),
                        sr.status.eq(ReservationStatus.ACCEPTED),
                        sr.isHidden.isFalse(),
                        sr.startDate.loe(endDate)
                                .and(sr.endDate.goe(startDate))
                )
                .fetchOne();

        return count != null ? count : 0L;
    }
}