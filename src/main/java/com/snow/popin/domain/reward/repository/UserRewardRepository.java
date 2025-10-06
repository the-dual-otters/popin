package com.snow.popin.domain.reward.repository;

import com.snow.popin.domain.reward.entity.UserReward;
import com.snow.popin.domain.reward.constant.UserRewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserRewardRepository extends JpaRepository<UserReward, Long> {

    Optional<UserReward> findByUserIdAndMissionSetId(Long userId, UUID missionSetId);

    Optional<UserReward> findByUserIdAndMissionSetIdAndStatus(
            Long userId, UUID missionSetId, UserRewardStatus status);
}
