package com.snow.popin.domain.mission.controller;

import com.snow.popin.domain.mission.entity.Mission;
import com.snow.popin.domain.mission.dto.response.MissionDto;
import com.snow.popin.domain.mission.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/missions")
public class MissionApiController {

    private final MissionService missionService;

    @GetMapping("/{id}")
    public MissionDto get(@PathVariable UUID id) {
        Mission mission = missionService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("mission not found"));
        return MissionDto.from(mission);
    }

    @GetMapping
    public List<MissionDto> list(@RequestParam(required = false) UUID missionSetId) {
        List<Mission> missions = (missionSetId == null)
                ? missionService.findAll()
                : missionService.findByMissionSetId(missionSetId);
        return missions.stream()
                .map(MissionDto::from)
                .collect(Collectors.toList());
    }
}