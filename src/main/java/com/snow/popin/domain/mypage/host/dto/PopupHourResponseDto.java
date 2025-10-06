package com.snow.popin.domain.mypage.host.dto;

import com.snow.popin.domain.popup.entity.PopupHours;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopupHourResponseDto {
    private Integer dayOfWeek;   // 1:월 ~ 7:일
    private String openTime;     // "HH:mm" 문자열
    private String closeTime;    // "HH:mm" 문자열

    public static PopupHourResponseDto fromEntity(PopupHours entity) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return new PopupHourResponseDto(
                entity.getDayOfWeek(),
                entity.getOpenTime() != null ? entity.getOpenTime().format(formatter) : null,
                entity.getCloseTime() != null ? entity.getCloseTime().format(formatter) : null
        );
    }
}
