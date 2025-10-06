package com.snow.popin.domain.popup.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static com.snow.popin.domain.map.entity.QVenue.venue;
import static com.snow.popin.domain.popup.entity.QPopup.popup;
import static com.snow.popin.domain.category.entity.QCategory.category;

@Repository
@RequiredArgsConstructor
public class PopupQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 전체 팝업 조회 (상태별 필터링)
     */
    public Page<Popup> findAllWithStatusFilter(PopupStatus status, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (status != null) {
            builder.and(popup.status.eq(status));
        }

        JPAQuery<Popup> query = queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(builder)
                .orderBy(popup.createdAt.desc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(popup.count())
                .from(popup)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 인기 팝업 조회 (조회수 기준)
     */
    public Page<Popup> findPopularActivePopups(Pageable pageable) {
        JPAQuery<Popup> query = queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .orderBy(
                        popup.viewCount.desc(),
                        popup.createdAt.desc()
                );

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(popup.count())
                .from(popup)
                .where(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 마감임박 팝업 조회
     */
    public Page<Popup> findDeadlineSoonPopups(PopupStatus status, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);

        if (status != null) {
            builder.and(popup.status.eq(status));
        }

        builder.and(popup.endDate.goe(today))
                .and(popup.endDate.loe(weekFromNow));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(builder)
                .orderBy(popup.endDate.asc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(popup.count())
                .from(popup)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 지역별 + 기간별 필터링
     */
    public Page<Popup> findByRegionAndDateRange(String region, LocalDate startDate,
                                                LocalDate endDate, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // 지역 필터
        if (StringUtils.hasText(region) && !"전체".equals(region)) {
            builder.and(venue.region.like("%" + region + "%"));
        }

        // 시작날짜 필터 (팝업 종료일이 검색 시작일보다 이후)
        if (startDate != null) {
            builder.and(popup.endDate.goe(startDate));
        }

        // 종료날짜 필터 (팝업 시작일이 검색 종료일보다 이전)
        if (endDate != null) {
            builder.and(popup.startDate.loe(endDate));
        }

        JPAQuery<Popup> query = queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(builder)
                .orderBy(popup.createdAt.desc());

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(popup.count())
                .from(popup)
                .leftJoin(popup.venue, venue)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 유사 팝업 조회
     */
    public Page<Popup> findSimilarPopups(String categoryName, Long excludeId, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(popup.category.name.eq(categoryName))
                .and(popup.id.ne(excludeId))
                .and(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(builder)
                .orderBy(
                        popup.viewCount.desc(),
                        popup.createdAt.desc()
                );

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(popup.count())
                .from(popup)
                .leftJoin(popup.category, category)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 카테고리별 추천 팝업 조회
     */
    public Page<Popup> findRecommendedPopupsByCategories(List<Long> categoryIds, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (categoryIds != null && !categoryIds.isEmpty()) {
            builder.and(popup.category.id.in(categoryIds));
        }

        builder.and(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(builder)
                .orderBy(
                        popup.viewCount.desc(),
                        popup.createdAt.desc()
                );

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(popup.count())
                .from(popup)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 카테고리명으로 팝업 조회
     */
    public Page<Popup> findByCategoryName(String categoryName, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(categoryName)) {
            builder.and(popup.category.name.eq(categoryName));
        }

        builder.and(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED));

        JPAQuery<Popup> query = queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(builder)
                .orderBy(
                        popup.viewCount.desc(),
                        popup.createdAt.desc()
                );

        List<Popup> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(popup.count())
                .from(popup)
                .leftJoin(popup.category, category)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * ID 목록으로 팝업 조회 (AI 추천 결과용)
     * Fetch Join으로 N+1 문제 해결
     */
    public List<Popup> findByIdIn(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()      // N+1 방지
                .leftJoin(popup.category, category).fetchJoin() // N+1 방지
                .where(popup.id.in(ids))
                .fetch();
    }


    /**
     * 지역별 팝업 조회
     */
    public List<Popup> findByRegion(String region) {
        return queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(
                        venue.region.eq(region)
                                .and(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                )
                .orderBy(
                        popup.viewCount.desc(),
                        popup.createdAt.desc()
                )
                .fetch();
    }

    /**
     * 지도용 팝업 조회
     */
    public List<Popup> findPopupsForMap(String region, List<Long> categoryIds) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .and(venue.latitude.isNotNull())
                .and(venue.longitude.isNotNull());

        if (StringUtils.hasText(region) && !"전체".equals(region)) {
            builder.and(venue.region.eq(region));
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            builder.and(popup.category.id.in(categoryIds));
        }

        return queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(builder)
                .orderBy(popup.createdAt.desc())
                .fetch();
    }

    /**
     * 경계 내 팝업 조회
     */
    public List<Popup> findPopupsInBounds(double southWestLat, double southWestLng,
                                          double northEastLat, double northEastLng) {
        return queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(
                        popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED)
                                .and(venue.latitude.isNotNull())
                                .and(venue.longitude.isNotNull())
                                .and(venue.latitude.between(southWestLat, northEastLat))
                                .and(venue.longitude.between(southWestLng, northEastLng))
                )
                .orderBy(popup.createdAt.desc())
                .fetch();
    }

    /**
     * 반경 내 팝업 조회
     */
    public List<Popup> findPopupsWithinRadius(double lat, double lng, double radiusKm) {
        // 대략적인 경계 박스 계산으로 1차 필터링
        double latDelta = radiusKm / 111.0; // 1도 ≈ 111km
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));

        List<Popup> candidates = queryFactory
                .selectFrom(popup)
                .leftJoin(popup.venue, venue).fetchJoin()
                .leftJoin(popup.category, category).fetchJoin()
                .where(
                        popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED)
                                .and(venue.latitude.isNotNull())
                                .and(venue.longitude.isNotNull())
                                .and(venue.latitude.between(lat - latDelta, lat + latDelta))
                                .and(venue.longitude.between(lng - lngDelta, lng + lngDelta))
                )
                .fetch();

        return candidates.stream()
                .filter(p -> calculateDistance(lat, lng,
                        p.getVenue().getLatitude(), p.getVenue().getLongitude()) <= radiusKm)
                .sorted((p1, p2) -> Double.compare(
                        calculateDistance(lat, lng, p1.getVenue().getLatitude(), p1.getVenue().getLongitude()),
                        calculateDistance(lat, lng, p2.getVenue().getLatitude(), p2.getVenue().getLongitude())
                ))
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 지도 통계
     */
    public List<Object[]> findMapPopupStatsByCategory(String region) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .and(venue.latitude.isNotNull())
                .and(venue.longitude.isNotNull());

        if (StringUtils.hasText(region) && !"전체".equals(region)) {
            builder.and(venue.region.eq(region));
        }

        return queryFactory
                .select(category.name, popup.count())
                .from(popup)
                .leftJoin(popup.category, category)
                .leftJoin(popup.venue, venue)
                .where(builder)
                .groupBy(category.id, category.name)
                .orderBy(popup.count().desc())
                .fetch()
                .stream()
                .map(tuple -> new Object[]{tuple.get(category.name), tuple.get(popup.count())})
                .collect(Collectors.toList());
    }

    /**
     * 지역별 지도 통계
     */
    public List<Object[]> findMapPopupStatsByRegion() {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                .and(venue.latitude.isNotNull())
                .and(venue.longitude.isNotNull())
                .and(venue.region.isNotNull());

        return queryFactory
                .select(venue.region, popup.count())
                .from(popup)
                .leftJoin(popup.venue, venue)
                .where(builder)
                .groupBy(venue.region)
                .orderBy(popup.count().desc())
                .fetch()
                .stream()
                .map(tuple -> new Object[]{tuple.get(venue.region), tuple.get(popup.count())})
                .collect(Collectors.toList());
    }

    /**
     * 진행중으로 업데이트할 팝업 조회
     */
    public List<Popup> findPopupsToUpdateToOngoing(LocalDate today) {
        return queryFactory
                .selectFrom(popup)
                .where(
                        popup.status.ne(PopupStatus.ONGOING)
                                .and(popup.startDate.loe(today))
                                .and(popup.endDate.goe(today))
                )
                .fetch();
    }

    /**
     * 종료로 업데이트할 팝업 조회
     */
    public List<Popup> findPopupsToUpdateToEnded(LocalDate today) {
        return queryFactory
                .selectFrom(popup)
                .where(
                        popup.status.ne(PopupStatus.ENDED)
                                .and(popup.endDate.lt(today))
                )
                .fetch();
    }

    // 거리 계산 헬퍼 메서드 (Haversine formula)
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // 지구 반지름 (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}