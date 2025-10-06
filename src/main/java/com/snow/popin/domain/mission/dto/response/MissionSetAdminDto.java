package com.snow.popin.domain.mission.dto.response;

import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.constant.MissionSetStatus;
import com.snow.popin.domain.reward.dto.request.RewardOptionDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class MissionSetAdminDto {
    private UUID id;
    private Long popupId;
    private Integer requiredCount;
    private MissionSetStatus status;
    private String rewardPin;
    private LocalDateTime createdAt;
    private List<MissionDto> missions;
    private String qrImageUrl;
    private List<RewardOptionDto> rewards;

    public static MissionSetAdminDto from(MissionSet set) {
        return MissionSetAdminDto.builder()
                .id(set.getId())
                .popupId(set.getPopupId())
                .requiredCount(set.getRequiredCount())
                .status(set.getStatus())
                .rewardPin(set.getRewardPin())
                .createdAt(set.getCreatedAt())
                .missions(set.getMissions().stream()
                        .map(MissionDto::from)
                        .collect(Collectors.toList()))
                .qrImageUrl(set.getQrImageUrl())
                .rewards(set.getRewards() != null
                        ? set.getRewards().stream()
                        .map(RewardOptionDto::from)
                        .collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
}
