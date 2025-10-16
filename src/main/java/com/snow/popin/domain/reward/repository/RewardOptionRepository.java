package com.snow.popin.domain.reward.repository;

import com.snow.popin.domain.reward.entity.RewardOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardOptionRepository extends JpaRepository<RewardOption, Long> {

    // missionSetId 기준 조회 (연관관계 경로)
    List<RewardOption> findByMissionSet_Id(UUID missionSetId);

    // 재고 차감 등 동시성 제어용 비관적 락 (SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "javax.persistence.lock.timeout", value = "5000"))
    Optional<RewardOption> findById(Long id);
}
