package com.snow.popin.domain.popup.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.category.entity.QCategory;
import com.snow.popin.domain.map.entity.QVenue;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.entity.QPopup;
import com.snow.popin.domain.popup.entity.QTag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PopupRepositoryImpl implements PopupRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QPopup p = QPopup.popup;
    private final QVenue v = QVenue.venue;
    private final QCategory c = QCategory.category;
    private final QTag t = QTag.tag;

    // ---------------- 기본 커스텀 ----------------

    @Override
    public List<Popup> findByStatus(PopupStatus status) {
        return queryFactory
                .selectFrom(p)
                .where(p.status.eq(status))
                .fetch();
    }

    @Override
    public Optional<Popup> findByIdWithDetails(Long id) {
        Popup result = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .leftJoin(p.images).fetchJoin()
                .leftJoin(p.hours).fetchJoin()
                .leftJoin(p.tags).fetchJoin()
                .where(p.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Page<Popup> findByBrandId(Long brandId, Pageable pageable) {
        List<Popup> content = queryFactory
                .selectFrom(p)
                .where(p.brandId.eq(brandId))
                .orderBy(p.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .where(p.brandId.eq(brandId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Optional<Popup> findByIdWithTagsAndCategory(Long id) {
        Popup result = queryFactory
                .selectFrom(p)
                .leftJoin(p.tags, t).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(p.id.eq(id))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    // ---------------- 확장 쿼리 ----------------

    @Override
    public Page<Popup> findAllWithStatusFilter(PopupStatus status, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        if (status != null) builder.and(p.status.eq(status));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(builder)
                .orderBy(p.createdAt.desc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Page<Popup> findPopularActivePopups(Pageable pageable) {
        List<Popup> content = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .orderBy(p.viewCount.desc(), p.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .where(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Page<Popup> findDeadlineSoonPopups(PopupStatus status, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);

        if (status != null) builder.and(p.status.eq(status));
        builder.and(p.endDate.goe(today))
                .and(p.endDate.loe(weekFromNow));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(builder)
                .orderBy(p.endDate.asc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Page<Popup> findByRegionAndDateRange(String region, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(region) && !"전체".equals(region)) {
            builder.and(v.region.like("%" + region + "%"));
        }
        if (startDate != null) {
            builder.and(p.endDate.goe(startDate)); // 종료일이 검색 시작일 이후
        }
        if (endDate != null) {
            builder.and(p.startDate.loe(endDate)); // 시작일이 검색 종료일 이전
        }

        JPAQuery<Popup> query = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(builder)
                .orderBy(p.createdAt.desc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .leftJoin(p.venue, v)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Page<Popup> findSimilarPopups(String categoryName, Long excludeId, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(p.category.name.eq(categoryName))
                .and(p.id.ne(excludeId))
                .and(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(builder)
                .orderBy(p.viewCount.desc(), p.createdAt.desc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .leftJoin(p.category, c)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Page<Popup> findRecommendedPopupsByCategories(List<Long> categoryIds, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            builder.and(p.category.id.in(categoryIds));
        }
        builder.and(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(builder)
                .orderBy(p.viewCount.desc(), p.createdAt.desc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public Page<Popup> findByCategoryName(String categoryName, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(categoryName)) {
            builder.and(p.category.name.eq(categoryName));
        }
        builder.and(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(builder)
                .orderBy(p.viewCount.desc(), p.createdAt.desc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .leftJoin(p.category, c)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public List<Popup> findByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        return queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(p.id.in(ids))
                .fetch();
    }

    @Override
    public List<Popup> findByRegion(String region) {
        return queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(
                        v.region.eq(region)
                                .and(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                )
                .orderBy(p.viewCount.desc(), p.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Popup> findPopupsForMap(String region, List<Long> categoryIds) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .and(v.latitude.isNotNull())
                .and(v.longitude.isNotNull());

        if (StringUtils.hasText(region) && !"전체".equals(region)) {
            builder.and(v.region.eq(region));
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            builder.and(p.category.id.in(categoryIds));
        }

        return queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(builder)
                .orderBy(p.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Popup> findPopupsInBounds(double southWestLat, double southWestLng,
                                          double northEastLat, double northEastLng) {
        return queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(
                        p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED)
                                .and(v.latitude.isNotNull())
                                .and(v.longitude.isNotNull())
                                .and(v.latitude.between(southWestLat, northEastLat))
                                .and(v.longitude.between(southWestLng, northEastLng))
                )
                .orderBy(p.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Popup> findPopupsWithinRadius(double lat, double lng, double radiusKm) {
        // 대략적인 경계 박스로 1차 후보 추림
        double latDelta = radiusKm / 111.0; // 위도 1도 ≈ 111km
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<Popup> candidates = queryFactory
                .selectFrom(p)
                .leftJoin(p.venue, v).fetchJoin()
                .leftJoin(p.category, c).fetchJoin()
                .where(
                        p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED)
                                .and(v.latitude.isNotNull())
                                .and(v.longitude.isNotNull())
                                .and(v.latitude.between(lat - latDelta, lat + latDelta))
                                .and(v.longitude.between(lng - lngDelta, lng + lngDelta))
                )
                .fetch();

        // 실제 거리(Haversine)로 반경 내만 남기고 거리순 정렬
        return candidates.stream()
                .filter(pp -> pp.getVenue() != null
                        && pp.getVenue().getLatitude() != null
                        && pp.getVenue().getLongitude() != null
                        && calculateDistance(lat, lng,
                        pp.getVenue().getLatitude(), pp.getVenue().getLongitude()) <= radiusKm)
                .sorted((a, b) -> Double.compare(
                        calculateDistance(lat, lng, a.getVenue().getLatitude(), a.getVenue().getLongitude()),
                        calculateDistance(lat, lng, b.getVenue().getLatitude(), b.getVenue().getLongitude())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Object[]> findMapPopupStatsByCategory(String region) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .and(v.latitude.isNotNull())
                .and(v.longitude.isNotNull());

        if (StringUtils.hasText(region) && !"전체".equals(region)) {
            builder.and(v.region.eq(region));
        }

        return queryFactory
                .select(c.name, p.count())
                .from(p)
                .leftJoin(p.category, c)
                .leftJoin(p.venue, v)
                .where(builder)
                .groupBy(c.id, c.name)
                .orderBy(p.count().desc())
                .fetch()
                .stream()
                .map(tuple -> new Object[]{tuple.get(c.name), tuple.get(p.count())})
                .collect(Collectors.toList());
    }

    @Override
    public List<Object[]> findMapPopupStatsByRegion() {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(p.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .and(v.latitude.isNotNull())
                .and(v.longitude.isNotNull())
                .and(v.region.isNotNull());

        return queryFactory
                .select(v.region, p.count())
                .from(p)
                .leftJoin(p.venue, v)
                .where(builder)
                .groupBy(v.region)
                .orderBy(p.count().desc())
                .fetch()
                .stream()
                .map(tuple -> new Object[]{tuple.get(v.region), tuple.get(p.count())})
                .collect(Collectors.toList());
    }

    @Override
    public List<Popup> findPopupsToUpdateToOngoing(LocalDate today) {
        return queryFactory
                .selectFrom(p)
                .where(
                        p.status.ne(PopupStatus.ONGOING)
                                .and(p.startDate.loe(today))
                                .and(p.endDate.goe(today))
                )
                .fetch();
    }

    @Override
    public List<Popup> findPopupsToUpdateToEnded(LocalDate today) {
        return queryFactory
                .selectFrom(p)
                .where(
                        p.status.ne(PopupStatus.ENDED)
                                .and(p.endDate.lt(today))
                )
                .fetch();
    }

    // ---------------- 유틸 ----------------
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double cVal = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * cVal;
    }
}
