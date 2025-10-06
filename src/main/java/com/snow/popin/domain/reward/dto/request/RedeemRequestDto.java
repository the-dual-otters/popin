package com.snow.popin.domain.reward.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class RedeemRequestDto {

    @NotNull
    private UUID missionSetId;

    @NotBlank
    private String staffPin;
}
