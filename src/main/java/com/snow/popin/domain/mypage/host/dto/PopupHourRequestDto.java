package com.snow.popin.domain.mypage.host.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopupHourRequestDto {
    private Integer dayOfWeek;  // 1=월, 7=일
    private String openTime;    // HH:mm
    private String closeTime;   // HH:mm
}
