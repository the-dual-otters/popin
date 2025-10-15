package com.snow.popin.domain.mission.repository;

import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.constant.MissionSetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MissionSetRepository extends JpaRepository<MissionSet, UUID>, MissionSetRepositoryCustom {
    Page<MissionSet> findByPopupId(Long popupId, Pageable pageable);
    Page<MissionSet> findByStatus(MissionSetStatus status, Pageable pageable);
}
