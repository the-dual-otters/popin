package com.snow.popin.domain.admin.service;

import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.repository.MissionSetRepository;
import com.snow.popin.domain.reward.dto.request.RewardOptionDto;
import com.snow.popin.domain.reward.entity.RewardOption;
import com.snow.popin.domain.reward.repository.RewardOptionRepository;
import com.snow.popin.global.exception.MissionException;
import com.snow.popin.global.exception.RewardException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRewardService {

    private final RewardOptionRepository rewardOptionRepository;
    private final MissionSetRepository missionSetRepository;

    @Transactional(readOnly = true)
    public List<RewardOptionDto> list(UUID missionSetId) {
        return rewardOptionRepository.findByMissionSet_Id(missionSetId).stream()
                .map(RewardOptionDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public RewardOptionDto create(UUID missionSetId, RewardOptionDto dto) {
        MissionSet set = missionSetRepository.findById(missionSetId)
                .orElseThrow(MissionException.MissionSetNotFound::new);

        RewardOption option = RewardOption.builder()
                .missionSet(set)
                .name(dto.getName())
                .total(dto.getTotal())
                .build();

        rewardOptionRepository.save(option);
        return RewardOptionDto.from(option);
    }

    @Transactional
    public RewardOptionDto update(Long optionId, RewardOptionDto dto) {
        RewardOption option = rewardOptionRepository.findById(optionId)
                .orElseThrow(RewardException.OptionNotFound::new);

        option.update(dto.getName(), dto.getTotal());
        return RewardOptionDto.from(option);
    }

    @Transactional
    public void delete(Long optionId) {
        RewardOption option = rewardOptionRepository.findById(optionId)
                .orElseThrow(RewardException.OptionNotFound::new);
        rewardOptionRepository.delete(option);
    }
}
