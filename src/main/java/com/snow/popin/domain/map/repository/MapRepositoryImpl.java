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
public class MapRepositoryImpl implements MapRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<String> findDistinctRegionsWithActivePopups() {
        // venue.region 이 null 아닌 지역 중에서, 진행중/예정 팝업이 존재하는 지역만
        return queryFactory
                .selectDistinct(venue.region)
                .from(venue)
                .where(
                        venue.region.isNotNull()
                                .and(
                                        queryFactory
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

    @Override
    public List<String> findDistinctRegionsWithCoordinates() {
        // 위경도 값이 존재하는 venue 들의 지역 목록
        return queryFactory
                .selectDistinct(venue.region)
                .from(venue)
                .where(
                        venue.region.isNotNull(),
                        venue.latitude.isNotNull(),
                        venue.longitude.isNotNull()
                )
                .orderBy(venue.region.asc())
                .fetch();
    }
}
