package com.snow.popin.domain.admin.controller;

import com.snow.popin.domain.admin.service.AdminMissionService;
import com.snow.popin.domain.mission.dto.request.MissionCreateRequestDto;
import com.snow.popin.domain.mission.dto.response.MissionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/mission")
public class AdminMissionApiController {

    private final AdminMissionService adminMissionService;

    /**
     * 미션 추가
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/{id}")
    public MissionDto addMission(@PathVariable UUID id, @RequestBody MissionCreateRequestDto request) {
        return adminMissionService.addMission(id, request);
    }

    /**
     * 미션 삭제
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMission(@PathVariable UUID id) {
        adminMissionService.deleteMission(id);
        return ResponseEntity.ok().build();
    }
}
