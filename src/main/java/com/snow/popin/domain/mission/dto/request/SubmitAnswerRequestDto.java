package com.snow.popin.domain.mission.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class SubmitAnswerRequestDto {
    private UUID missionId;
    private String answer;

    @Builder
    public SubmitAnswerRequestDto(UUID missionId, String answer) {
        this.missionId = missionId;
        this.answer = answer;
    }
}
