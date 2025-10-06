package com.snow.popin.domain.reward.repository;

import com.snow.popin.domain.reward.entity.RewardOption;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.*;

public interface RewardOptionRepository extends JpaRepository<RewardOption, Long> {

    // missionSetId 기반 조회 (연관관계 경로 탐색)
    List<RewardOption> findByMissionSet_Id(UUID missionSetId);

    /** 재고 차감 시 동시성 제어 (비관적 락) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from RewardOption o where o.id = :id")
    Optional<RewardOption> lockById(@Param("id") Long id);
}
