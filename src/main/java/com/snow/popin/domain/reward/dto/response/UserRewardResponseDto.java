package com.snow.popin.domain.reward.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRewardResponseDto {
    private boolean ok;
    private String status;   // ISSUED / REDEEMED / CANCELED
    private Long optionId;
    private String optionName;
}
