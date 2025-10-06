package com.snow.popin.domain.mission.controller;

import com.snow.popin.domain.mission.dto.response.ActiveMissionSetResponseDto;
import com.snow.popin.domain.mission.entity.UserMission;
import com.snow.popin.domain.mission.service.UserMissionService;
import com.snow.popin.domain.user.service.UserService;
import com.snow.popin.domain.mission.dto.request.SubmitAnswerRequestDto;
import com.snow.popin.domain.mission.dto.response.SubmitAnswerResponseDto;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-missions")
@Validated
public class UserMissionApiController {

    private final UserMissionService userMissionService;
    private final UserUtil userUtil;
    private final UserService userService;


    @GetMapping("/{id}")
    public ResponseEntity<UserMission> get(@PathVariable Long id) {
        Optional<UserMission> found = userMissionService.findById(id);
        if (found.isPresent()) return ResponseEntity.ok(found.get());
        return ResponseEntity.notFound().build();
    }



    @PostMapping("/{missionId}/submit-answer")
    public ResponseEntity<SubmitAnswerResponseDto> submitAnswer(
            @PathVariable UUID missionId,
            @RequestBody @Valid SubmitAnswerRequestDto req
    ) {
        // 현재 로그인한 사용자 ID 가져오기
        Long userId = userUtil.getCurrentUserId();

        // 인증 안 된 경우
        if (userId == null) {
            return ResponseEntity
                    .status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .build();
        }

        SubmitAnswerResponseDto res = userMissionService.submitAnswer(missionId, userId, req.getAnswer());
        return ResponseEntity.ok(res);
    }


    @GetMapping("/my-missions")
    public ResponseEntity<List<ActiveMissionSetResponseDto>> getMyMissions() {
        Long userId = userUtil.getCurrentUserId();
        List<ActiveMissionSetResponseDto> result = userMissionService.getMyMissionPopups(userId);
        return ResponseEntity.ok(result);
    }
}
