package com.snow.popin.domain.map.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.snow.popin.domain.map.entity.QVenue.venue;
import static com.snow.popin.domain.popup.entity.QPopup.popup;

@Repository
@RequiredArgsConstructor
public class MapQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 현재 진행중이거나 예정인 팝업이 있는 지역 목록 조회
     */
    public List<String> findDistinctRegionsWithActivePopups() {
        return queryFactory
                .selectDistinct(venue.region)
                .from(venue)
                .where(
                        venue.region.isNotNull()
                                .and(queryFactory
                                        .selectOne()
                                        .from(popup)
                                        .where(
                                                popup.venue.eq(venue)
                                                        .and(popup.status.in(PopupStatus.ONGOING, PopupStatus.PLANNED))
                                        )
                                        .exists()
                                )
                )
                .orderBy(venue.region.asc())
                .fetch();
    }

    /**
     * 좌표가 있는 장소들의 지역 목록 조회
     */
    public List<String> findDistinctRegionsWithCoordinates() {
        return queryFactory
                .selectDistinct(venue.region)
                .from(venue)
                .where(
                        venue.region.isNotNull()
                                .and(venue.latitude.isNotNull())
                                .and(venue.longitude.isNotNull())
                )
                .orderBy(venue.region.asc())
                .fetch();
    }
}