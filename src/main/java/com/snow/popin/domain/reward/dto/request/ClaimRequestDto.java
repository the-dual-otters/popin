package com.snow.popin.domain.reward.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class ClaimRequestDto {

    @NotNull
    private UUID missionSetId;

    @NotNull
    private Long optionId;
}
