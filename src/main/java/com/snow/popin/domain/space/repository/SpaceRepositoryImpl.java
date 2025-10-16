package com.snow.popin.domain.space.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.space.entity.QSpace;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.QUser;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.map.entity.QVenue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpaceRepositoryImpl implements SpaceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QSpace s = QSpace.space;
    private final QUser o = QUser.user;      // owner
    private final QVenue v = QVenue.venue;

    @Override
    public Page<Space> findPublicVisibleOrderByCreatedAtDesc(Pageable pageable) {
        List<Space> content = queryFactory
                .selectFrom(s)
                .leftJoin(s.owner, o).fetchJoin()
                .leftJoin(s.venue, v).fetchJoin()
                .where(s.isPublic.isTrue()
                        .and(s.isHidden.isFalse()))
                .orderBy(s.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(s.count())
                .from(s)
                .where(s.isPublic.isTrue().and(s.isHidden.isFalse()))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDescWithJoins(User owner) {
        return queryFactory
                .selectFrom(s)
                .join(s.owner, o).fetchJoin()
                .leftJoin(s.venue, v).fetchJoin()
                .where(s.owner.eq(owner)
                        .and(s.isHidden.isFalse()))
                .orderBy(s.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Space> searchSpacesWithJoins(String keyword, String location, Integer minArea, Integer maxArea) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(s.isPublic.isTrue())
                .and(s.isHidden.isFalse());

        // 키워드: 제목/설명 like (대/소문자 무시)
        if (StringUtils.hasText(keyword)) {
            String q = keyword.trim();
            where.and(
                    s.title.containsIgnoreCase(q)
                            .or(s.description.containsIgnoreCase(q))
            );
        }

        // 위치: Space.address 또는 Venue.* like
        if (StringUtils.hasText(location)) {
            String loc = location.trim();
            where.and(
                    s.address.containsIgnoreCase(loc)
                            .or(v.roadAddress.containsIgnoreCase(loc))
                            .or(v.jibunAddress.containsIgnoreCase(loc))
                            .or(v.detailAddress.containsIgnoreCase(loc))
            );
        }

        // 면적 범위
        if (minArea != null) where.and(s.areaSize.goe(minArea));
        if (maxArea != null) where.and(s.areaSize.loe(maxArea));

        return queryFactory
                .selectDistinct(s)
                .from(s)
                .join(s.owner, o).fetchJoin()
                .leftJoin(s.venue, v).fetchJoin()
                .where(where)
                .orderBy(s.createdAt.desc())
                .fetch();
    }
}
