package com.snow.popin.domain.popup.dto.response;

import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.popup.entity.Popup;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class ActiveMissionPopupResponseDto {
    private Long popupId;
    private String popupTitle;
    private String summary;
    private String mainImageUrl;
    private String region;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodText;

    private UUID missionSetId;

    // 진행 상태 대신 "cleared" 값
    private boolean cleared;

    public static ActiveMissionPopupResponseDto from(MissionSet set, boolean cleared) {
        Popup popup = set.getPopup();
        if (popup == null) {
            throw new IllegalStateException("MissionSet에 연결된 Popup이 없습니다.");
        }

        return ActiveMissionPopupResponseDto.builder()
                .popupId(popup.getId())
                .popupTitle(popup.getTitle())
                .summary(popup.getSummary())
                .mainImageUrl(popup.getMainImageUrl())
                .region(popup.getRegion())
                .description(popup.getDescription())
                .startDate(popup.getStartDate())
                .endDate(popup.getEndDate())
                .periodText(popup.getPeriodText())
                .missionSetId(set.getId())
                .cleared(cleared)
                .build();
    }
}

