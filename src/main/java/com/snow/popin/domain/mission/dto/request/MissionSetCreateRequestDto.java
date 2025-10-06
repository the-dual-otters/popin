package com.snow.popin.domain.mission.dto.request;

import com.snow.popin.domain.mission.constant.MissionSetStatus;
import lombok.Getter;

@Getter
public class MissionSetCreateRequestDto {
    private Long popupId;
    private Integer requiredCount;
    private MissionSetStatus status;
    private String rewardPin;
}