package com.snow.popin.domain.mission.repository;

import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.constant.MissionSetStatus;
import com.snow.popin.domain.mission.entity.UserMission;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MissionSetRepository extends JpaRepository<MissionSet, UUID> {
    Page<MissionSet> findByPopupId(Long popupId, Pageable pageable);
    Page<MissionSet> findByStatus(MissionSetStatus status, Pageable pageable);

    @Query("select um from UserMission um " +
            "join fetch um.mission m " +
            "join fetch m.missionSet ms " +
            "where um.user = :user")
    List<UserMission> findAllByUserWithMissionSet(User user);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MissionSet ms set ms.status = 'ENABLED', ms.completedAt = null " +
            "where ms.popupId in :popupIds")
    int bulkEnableByPopupIds(@Param("popupIds") Collection<Long> popupIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MissionSet ms set ms.status = 'DISABLED', ms.completedAt = CURRENT_TIMESTAMP " +
            "where ms.popupId in :popupIds")
    int bulkDisableByPopupIds(@Param("popupIds") Collection<Long> popupIds);
}
