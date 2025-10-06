package com.snow.popin.domain.reward.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor @NoArgsConstructor @Builder
public class RedeemResponseDto {
    private boolean ok;
    private String status;            // REDEEMED
    private LocalDateTime redeemedAt; // 수령 시각
    private String error;
}
