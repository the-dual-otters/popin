package com.snow.popin.domain.reward.service;

import com.snow.popin.domain.mission.constant.UserMissionStatus;
import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.repository.MissionSetRepository;
import com.snow.popin.domain.mission.repository.UserMissionRepository;
import com.snow.popin.domain.reward.constant.UserRewardStatus;
import com.snow.popin.domain.reward.entity.RewardOption;
import com.snow.popin.domain.reward.entity.UserReward;
import com.snow.popin.domain.reward.repository.RewardOptionRepository;
import com.snow.popin.domain.reward.repository.UserRewardRepository;
import com.snow.popin.global.exception.MissionException;
import com.snow.popin.global.exception.RewardException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardOptionRepository rewardOptionRepository;
    private final UserRewardRepository rewardRepository;
    private final MissionSetRepository missionSetRepository;
    private final UserMissionRepository userMissionRepository;

    @Transactional(readOnly = true)
    public List<RewardOption> listOptions(UUID missionSetId) {
        return rewardOptionRepository.findByMissionSet_Id(missionSetId);
    }

    /**
     * 발급: 유저당 1회 / 미션 조건 충족 / 옵션 재고 차감
     */
    @Transactional
    public UserReward claim(UUID missionSetId, Long optionId, Long userId) {
        // 이미 발급된 게 있으면 그대로 반환 (idempotent)
        var existing = rewardRepository.findByUserIdAndMissionSetId(userId, missionSetId);
        if (existing.isPresent()) {
            throw new RewardException.AlreadyClaimed();
        }

        // 미션 조건 확인
        MissionSet missionSet = missionSetRepository.findById(missionSetId)
                .orElseThrow(MissionException.MissionSetNotFound::new);
        int required = Optional.ofNullable(missionSet.getRequiredCount()).orElse(0);
        long success = userMissionRepository.countByUser_IdAndMission_MissionSet_IdAndStatus(
                userId, missionSetId, UserMissionStatus.COMPLETED);
        if (success < required) {
            throw new MissionException.MissionNotCleared();
        }

        // 옵션 잠금 + 재고 차감
        RewardOption opt = rewardOptionRepository.lockById(optionId)
                .orElseThrow(RewardException.OptionNotFound::new);

        if (!opt.getMissionSet().getId().equals(missionSetId)) {
            throw new RewardException.OptionNotInMissionSet();
        }

        opt.consumeOne(); // 재고 없으면 RewardException.OutOfStock 발생

        // 지급 레코드 생성
        UserReward userReward = UserReward.builder()
                .userId(userId)
                .missionSetId(missionSetId)
                .option(opt)
                .status(UserRewardStatus.ISSUED)
                .build();

        return rewardRepository.save(userReward);
    }

    /**
     * 리워드 수령
     */
    @Transactional
    public UserReward redeem(UUID missionSetId, Long userId, String staffPinPlain) {
        UserReward userReward = rewardRepository.findByUserIdAndMissionSetIdAndStatus(
                userId, missionSetId, UserRewardStatus.ISSUED
        ).orElseThrow(RewardException.NotIssued::new);

        MissionSet missionSet = missionSetRepository.findById(missionSetId)
                .orElseThrow(MissionException.MissionSetNotFound::new);

        String stored = missionSet.getRewardPin(); // 평문 저장 사용
        if (stored == null || stored.isBlank()) {
            throw new RewardException.NoStaffPin();
        }

        if (!Objects.equals(stored, staffPinPlain)) {
            throw new RewardException.InvalidStaffPin();
        }

        userReward.markRedeemed(); // 내부에서 status=REDEEMED, redeemedAt=now()

        return userReward;
    }

    @Transactional(readOnly = true)
    public Optional<UserReward> findUserReward(Long userId, UUID missionSetId) {
        return rewardRepository.findByUserIdAndMissionSetId(userId, missionSetId);
    }
}
