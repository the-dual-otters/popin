package com.snow.popin.domain.mission.repository;

import com.snow.popin.domain.mission.entity.UserMission;
import com.snow.popin.domain.mission.constant.UserMissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMissionRepository extends JpaRepository<UserMission, Long>, UserMissionRepositoryCustom {
    Optional<UserMission> findByUser_IdAndMission_Id(Long userId, UUID missionId);
    long countByUser_IdAndMission_MissionSet_IdAndStatus(Long userId, UUID missionSetId, UserMissionStatus status);
    List<UserMission> findByUser_IdAndMission_MissionSet_Id(Long userId, UUID missionSetId);
    List<UserMission> findByUser_Id(Long userId);
}
