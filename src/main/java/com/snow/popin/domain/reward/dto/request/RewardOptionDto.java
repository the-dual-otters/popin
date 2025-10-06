package com.snow.popin.domain.reward.dto.request;

import com.snow.popin.domain.reward.entity.RewardOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RewardOptionDto {
    private Long id;
    private UUID missionSetId;
    private String name;
    private int total;
    private int issued;
    private int remaining;

    public static RewardOptionDto from(RewardOption r) {
        return RewardOptionDto.builder()
                .id(r.getId())
                .missionSetId(r.getMissionSet() != null ? r.getMissionSet().getId() : null) // 변경됨
                .name(r.getName())
                .total(r.getTotal())
                .issued(r.getIssued())
                .remaining(r.getRemaining())
                .build();
    }
}
