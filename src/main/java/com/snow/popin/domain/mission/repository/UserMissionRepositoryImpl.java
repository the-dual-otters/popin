package com.snow.popin.domain.mission.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.mission.entity.QUserMission;
import com.snow.popin.domain.mission.constant.UserMissionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class UserMissionRepositoryImpl implements UserMissionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Long countCompletedMissionsByPopupAndDate(Long popupId, LocalDateTime start, LocalDateTime end) {
        QUserMission um = QUserMission.userMission;

        return queryFactory
                .select(um.count())
                .from(um)
                .join(um.mission).fetchJoin()
                .join(um.mission.missionSet).fetchJoin()
                .where(
                        um.mission.missionSet.popupId.eq(popupId)
                                .and(um.status.eq(UserMissionStatus.COMPLETED))
                                .and(um.completedAt.between(start, end))
                )
                .fetchOne();
    }
}
