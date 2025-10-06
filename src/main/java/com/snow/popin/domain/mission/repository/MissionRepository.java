package com.snow.popin.domain.mission.repository;

import com.snow.popin.domain.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
    List<Mission> findByMissionSet_Id(UUID missionSetId);
}
