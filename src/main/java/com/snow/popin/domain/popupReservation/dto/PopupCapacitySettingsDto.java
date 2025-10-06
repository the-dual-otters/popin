package com.snow.popin.domain.popupReservation.dto;

import com.snow.popin.domain.popupReservation.entity.PopupReservationSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PopupCapacitySettingsDto {

    @NotNull(message = "시간 슬롯당 최대 인원을 입력해주세요.")
    @Min(value = 1, message = "시간 슬롯당 최대 인원은 1명 이상이어야 합니다.")
    @Max(value = 100, message = "시간 슬롯당 최대 인원은 100명을 초과할 수 없습니다.")
    private Integer maxCapacityPerSlot;

    @NotNull(message = "시간 슬롯 간격을 입력해주세요.")
    @Min(value = 15, message = "시간 슬롯 간격은 최소 15분이어야 합니다.")
    @Max(value = 120, message = "시간 슬롯 간격은 최대 120분을 초과할 수 없습니다.")
    private Integer timeSlotInterval;

    //  메서드
    public static PopupCapacitySettingsDto from(PopupReservationSettings settings) {
        return PopupCapacitySettingsDto.builder()
                .maxCapacityPerSlot(settings.getMaxCapacityPerSlot())
                .timeSlotInterval(settings.getTimeSlotInterval())
                .build();
    }
}