package com.snow.popin.domain.mission.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.snow.popin.domain.mission.constant.MissionSetStatus;
import com.snow.popin.domain.mission.entity.QMissionSet;
import com.snow.popin.domain.mission.entity.QMission;
import com.snow.popin.domain.mission.entity.QUserMission;
import com.snow.popin.domain.mission.entity.UserMission;
import com.snow.popin.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MissionSetRepositoryImpl implements MissionSetRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QUserMission um = QUserMission.userMission;
    private final QMission m = QMission.mission;
    private final QMissionSet ms = QMissionSet.missionSet;

    @Override
    public List<UserMission> findAllByUserWithMissionSet(User user) {
        return queryFactory
                .selectFrom(um)
                .join(um.mission, m).fetchJoin()
                .join(m.missionSet, ms).fetchJoin()
                .where(um.user.eq(user))
                .fetch();
    }

    @Override
    public int bulkEnableByPopupIds(Collection<Long> popupIds) {
        return (int) queryFactory
                .update(ms)
                .set(ms.status, MissionSetStatus.ENABLED)
                .set(ms.completedAt, (LocalDateTime) null)
                .where(ms.popupId.in(popupIds))
                .execute();
    }

    @Override
    public int bulkDisableByPopupIds(Collection<Long> popupIds) {
        return (int) queryFactory
                .update(ms)
                .set(ms.status, MissionSetStatus.DISABLED)
                .set(ms.completedAt, LocalDateTime.now())
                .where(ms.popupId.in(popupIds))
                .execute();
    }
}
