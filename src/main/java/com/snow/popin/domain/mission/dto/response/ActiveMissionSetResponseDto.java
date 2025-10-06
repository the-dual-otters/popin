package com.snow.popin.domain.mission.dto.response;

import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.popup.dto.response.PopupBasicResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ActiveMissionSetResponseDto {

    private final UUID missionSetId;
    private final boolean cleared;

    private final PopupBasicResponseDto popup;

    public static ActiveMissionSetResponseDto from(MissionSet set, boolean cleared) {
        if (set.getPopup() == null) {
            throw new IllegalStateException("MissionSet에 연결된 Popup이 없습니다.");
        }

        return ActiveMissionSetResponseDto.builder()
                .missionSetId(set.getId())
                .cleared(cleared)
                .popup(PopupBasicResponseDto.from(set.getPopup()))
                .build();
    }
}
