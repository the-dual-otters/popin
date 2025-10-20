package com.snow.popin.domain.space.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.map.entity.QVenue;
import com.snow.popin.domain.space.entity.QSpace;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.QUser;
import com.snow.popin.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpaceRepositoryImpl implements SpaceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QSpace space = QSpace.space;
    private final QUser user = QUser.user;
    private final QVenue venue = QVenue.venue;

    @Override
    public List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDesc(User owner) {
        return queryFactory
                .selectFrom(space)
                .where(
                        space.owner.eq(owner),
                        space.isHidden.isFalse()
                )
                .orderBy(space.createdAt.desc())
                .fetch();
    }

    @Override
    public Optional<Space> findByIdAndOwner(Long id, User owner) {
        Space result = queryFactory
                .selectFrom(space)
                .where(
                        space.id.eq(id),
                        space.owner.eq(owner)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public long countByIsHidden(boolean isHidden) {
        Long count = queryFactory
                .select(space.count())
                .from(space)
                .where(space.isHidden.eq(isHidden))
                .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public List<Space> searchSpacesWithJoins(String keyword, String location, Integer minArea, Integer maxArea) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(space.isPublic.isTrue())
                .and(space.isHidden.isFalse());

        if (keyword != null && !keyword.isEmpty()) {
            BooleanBuilder keywordBuilder = new BooleanBuilder();
            keywordBuilder.or(space.title.toLowerCase().contains(keyword.toLowerCase()))
                    .or(space.description.toLowerCase().contains(keyword.toLowerCase()));
            builder.and(keywordBuilder);
        }

        if (location != null && !location.isEmpty()) {
            BooleanBuilder locationBuilder = new BooleanBuilder();
            locationBuilder.or(space.address.toLowerCase().contains(location.toLowerCase()))
                    .or(venue.roadAddress.toLowerCase().contains(location.toLowerCase()))
                    .or(venue.jibunAddress.toLowerCase().contains(location.toLowerCase()))
                    .or(venue.detailAddress.toLowerCase().contains(location.toLowerCase()));
            builder.and(locationBuilder);
        }

        if (minArea != null) {
            builder.and(space.areaSize.goe(minArea));
        }
        if (maxArea != null) {
            builder.and(space.areaSize.loe(maxArea));
        }

        return queryFactory
                .selectFrom(space)
                .distinct()
                .join(space.owner, user).fetchJoin()
                .leftJoin(space.venue, venue).fetchJoin()
                .where(builder)
                .orderBy(space.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDescWithJoins(User owner) {
        return queryFactory
                .selectFrom(space)
                .distinct()
                .join(space.owner, user).fetchJoin()
                .leftJoin(space.venue, venue).fetchJoin()
                .where(
                        space.owner.eq(owner),
                        space.isHidden.isFalse()
                )
                .orderBy(space.createdAt.desc())
                .fetch();
    }

    @Override
    public Page<Space> findByIsPublicTrueAndIsHiddenFalseOrderByCreatedAtDesc(Pageable pageable) {
        List<Space> content = queryFactory
                .selectFrom(space)
                .where(
                        space.isPublic.isTrue(),
                        space.isHidden.isFalse()
                )
                .orderBy(space.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(space.count())
                .from(space)
                .where(
                        space.isPublic.isTrue(),
                        space.isHidden.isFalse()
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}