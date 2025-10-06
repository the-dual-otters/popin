package com.snow.popin.domain.popupstat.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupStatsResponseDto {

    private LocalDate date;
    private Integer hour;
    private Integer visitorCount;
    private Integer reservationCount;
    private Integer canceledCount;
    private Integer missionCompletedCount;

}