package com.snow.popin.domain.reward.dto.response;

import lombok.*;

@Data
@AllArgsConstructor @NoArgsConstructor @Builder
public class ClaimResponseDto {
    private boolean ok;
    private Long rewardId;
    private String status;  // ISSUED
    private Long optionId;
}