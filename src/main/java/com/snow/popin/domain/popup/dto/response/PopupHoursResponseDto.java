package com.snow.popin.domain.popup.dto.response;

import com.snow.popin.domain.popup.entity.PopupHours;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;

@Builder
@Getter
public class PopupHoursResponseDto {
    private final Long id;
    private final Integer dayOfWeek;
    private final LocalTime openTime;
    private final LocalTime closeTime;
    private final String note;

    public String getDayOfWeekText() {
        String[] days = {"월", "화", "수", "목", "금", "토", "일"};
        return dayOfWeek != null && dayOfWeek >= 0 && dayOfWeek <= 6 ? days[dayOfWeek] : "";
    }

    public String getTimeRangeText() {
        if (openTime == null && closeTime == null) {
            return "시간 미정";
        }
        return String.format("%s - %s",
                openTime != null ? openTime.toString() : "미정",
                closeTime != null ? closeTime.toString() : "미정");
    }

    public static PopupHoursResponseDto from(PopupHours hours) {
        return PopupHoursResponseDto.builder()
                .id(hours.getId())
                .dayOfWeek(hours.getDayOfWeek())
                .openTime(hours.getOpenTime())
                .closeTime(hours.getCloseTime())
                .note(hours.getNote())
                .build();
    }
}