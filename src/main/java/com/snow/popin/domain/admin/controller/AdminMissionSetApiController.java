package com.snow.popin.domain.admin.controller;

import com.snow.popin.domain.admin.service.AdminMissionSetService;
import com.snow.popin.domain.mission.constant.MissionSetStatus;
import com.snow.popin.domain.mission.dto.request.MissionSetCreateRequestDto;
import com.snow.popin.domain.mission.dto.request.MissionSetUpdateRequestDto;
import com.snow.popin.domain.mission.dto.response.MissionSetAdminDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/mission-sets")
public class AdminMissionSetApiController {

    private final AdminMissionSetService adminMissionSetService;

    @GetMapping
    public Page<MissionSetAdminDto> list(Pageable pageable,
                                         @RequestParam(required = false) Long popupId,
                                         @RequestParam(required = false) MissionSetStatus status) {
        return adminMissionSetService.getMissionSets(pageable, popupId, status);
    }

    /**
     * 미션셋 상세
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public MissionSetAdminDto detail(@PathVariable UUID id) {
        return adminMissionSetService.getMissionSetDetail(id);
    }

    /**
     * 미션셋 생성
     * @param request
     * @return
     */
    @PostMapping
    public MissionSetAdminDto create(@RequestBody MissionSetCreateRequestDto request) {
        return adminMissionSetService.createMissionSet(request);
    }

    /**
     * 미션셋 삭제
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminMissionSetService.deleteMissionSet(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 미션셋 수정
     * @param id
     * @param request
     * @return
     */
    @PutMapping("/{id}")
    public MissionSetAdminDto updateMissionSet(
            @PathVariable UUID id,
            @RequestBody MissionSetUpdateRequestDto request
    ) {
        return adminMissionSetService.updateMissionSet(id, request);
    }
}
