package com.snow.popin.domain.reward.controller;

import com.snow.popin.domain.reward.dto.request.ClaimRequestDto;
import com.snow.popin.domain.reward.dto.request.RedeemRequestDto;
import com.snow.popin.domain.reward.dto.response.ClaimResponseDto;
import com.snow.popin.domain.reward.dto.response.OptionViewResponseDto;
import com.snow.popin.domain.reward.dto.response.RedeemResponseDto;
import com.snow.popin.domain.reward.dto.response.UserRewardResponseDto;
import com.snow.popin.domain.reward.entity.UserReward;
import com.snow.popin.domain.reward.service.RewardService;
import com.snow.popin.domain.user.service.UserService;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
@Validated
public class RewardApiController {

    private final RewardService rewardService;
    private final UserUtil userUtil;
    private final UserService userService;

    // 옵션 목록 (잔여량 포함)
    @GetMapping("/options/{missionSetId}")
    public List<OptionViewResponseDto> options(@PathVariable UUID missionSetId) {
        return rewardService.listOptions(missionSetId).stream()
                .map(o -> OptionViewResponseDto.builder()
                        .id(o.getId())
                        .name(o.getName())
                        .total(o.getTotal())
                        .issued(o.getIssued())
                        .remaining(Math.max(0, o.getTotal() - o.getIssued()))
                        .build()
                )
                .collect(Collectors.toList());
    }

    // 발급 (유저당 1회, 재고 차감)
    @PostMapping("/claim")
    public ResponseEntity<ClaimResponseDto> claim(@RequestBody @Valid ClaimRequestDto req) {
        Long userId = userUtil.getCurrentUserId();
        UserReward userReward = rewardService.claim(req.getMissionSetId(), req.getOptionId(), userId);

        return ResponseEntity.ok(
                ClaimResponseDto.builder()
                        .ok(true)
                        .rewardId(userReward.getId())
                        .status(userReward.getStatus().name())
                        .optionId(userReward.getOption().getId())
                        .build()
        );
    }

    // 수령 (PIN 입력)
    @PostMapping("/redeem")
    public ResponseEntity<RedeemResponseDto> redeem(@RequestBody @Valid RedeemRequestDto req) {
        Long userId = userUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserReward userReward = rewardService.redeem(req.getMissionSetId(), userId, req.getStaffPin());

        return ResponseEntity.ok(
                RedeemResponseDto.builder()
                        .ok(true)
                        .status(userReward.getStatus().name())
                        .redeemedAt(userReward.getRedeemedAt())
                        .build()
        );
    }

    // 내 리워드 조회 (이미 발급 받았는지 확인)
    @GetMapping("/my/{missionSetId}")
    public ResponseEntity<UserRewardResponseDto> myReward(@PathVariable UUID missionSetId) {
        Long userId = userUtil.getCurrentUserId();

        return rewardService.findUserReward(userId, missionSetId)
                .map(userReward -> ResponseEntity.ok(
                        UserRewardResponseDto.builder()
                                .ok(true)
                                .status(userReward.getStatus().name())
                                .optionId(userReward.getOption().getId())
                                .optionName(userReward.getOption().getName())
                                .build()
                ))
                .orElse(ResponseEntity.ok(UserRewardResponseDto.builder().ok(false).build()));
    }

}
