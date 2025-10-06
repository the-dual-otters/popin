package com.snow.popin.domain.mission.repository;

import com.snow.popin.domain.mission.entity.UserMission;
import com.snow.popin.domain.mission.constant.UserMissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {
    Optional<UserMission> findByUser_IdAndMission_Id(Long userId, UUID missionId);
    long countByUser_IdAndMission_MissionSet_IdAndStatus(Long userId, UUID missionSetId, UserMissionStatus status);
    List<UserMission> findByUser_IdAndMission_MissionSet_Id(Long userId, UUID missionSetId);
    List<UserMission> findByUser_Id(Long userId);
    /**
     * 특정 팝업의 특정 기간 내 완료된 미션 수 조회
     */
    @Query("SELECT COUNT(um) FROM UserMission um " +
            "JOIN um.mission m " +
            "JOIN m.missionSet ms " +
            "WHERE ms.popupId = :popupId " +
            "AND um.status = 'COMPLETED' " +
            "AND um.completedAt BETWEEN :start AND :end")
    Long countCompletedMissionsByPopupAndDate(
            @Param("popupId") Long popupId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
