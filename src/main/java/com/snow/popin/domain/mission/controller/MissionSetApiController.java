package com.snow.popin.domain.mission.controller;

import com.snow.popin.domain.mission.dto.response.MissionSetViewDto;
import com.snow.popin.domain.mission.service.MissionSetService;
import com.snow.popin.global.exception.MissionException;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mission-sets")
public class MissionSetApiController {

    private final MissionSetService missionSetService;
    private final UserUtil userUtil;

    @GetMapping("/{missionSetId}")
    public MissionSetViewDto byMissionSet(@PathVariable UUID missionSetId) {
        if (!userUtil.isAuthenticated()) {
            throw new MissionException.Unauthorized("인증된 사용자가 없습니다.");
        }

        Long userId = userUtil.getCurrentUserId();
        if (userId == null) {
            throw new MissionException.Unauthorized("현재 사용자 정보를 찾을 수 없습니다.");
        }

        return missionSetService.getOne(missionSetId, userId);
    }
}
