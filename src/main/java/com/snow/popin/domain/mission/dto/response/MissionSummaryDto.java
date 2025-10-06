package com.snow.popin.domain.mission.dto.response;

import com.snow.popin.domain.mission.constant.UserMissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MissionSummaryDto {
    private final UUID id;
    private final String title;
    private final String description;
    private final UserMissionStatus userStatus;  // PENDING / SUCCESS / FAIL
}
