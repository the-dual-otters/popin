package com.snow.popin.domain.admin.service;

import com.snow.popin.domain.mission.dto.request.MissionCreateRequestDto;
import com.snow.popin.domain.mission.dto.response.MissionDto;
import com.snow.popin.domain.mission.entity.Mission;
import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.repository.MissionRepository;
import com.snow.popin.domain.mission.repository.MissionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMissionService {

    private final MissionSetRepository missionSetRepository;
    private final MissionRepository missionRepository;


    /**
     * 미션 추가
     * @param setId
     * @param req
     * @return
     */
    public MissionDto addMission(UUID setId, MissionCreateRequestDto req) {
        MissionSet set = missionSetRepository.findById(setId)
                .orElseThrow(() -> new IllegalArgumentException("MissionSet not found"));
        Mission mission = Mission.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .answer(req.getAnswer())
                .missionSet(set)
                .build();
        missionRepository.save(mission);
        return MissionDto.from(mission);
    }

    /**
     * 미션 삭제
     * @param missionId
     */
    public void deleteMission(UUID missionId) {
        missionRepository.deleteById(missionId);
    }

}
