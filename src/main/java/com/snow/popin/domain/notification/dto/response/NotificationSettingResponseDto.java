package com.snow.popin.domain.notification.dto.response;

import com.snow.popin.domain.notification.entity.NotificationSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationSettingResponseDto {
    private boolean reservationEnabled;
    private boolean systemEnabled;
    private boolean eventEnabled;

    public static NotificationSettingResponseDto from(NotificationSetting setting) {
        return new NotificationSettingResponseDto(
                setting.isReservationEnabled(),
                setting.isSystemEnabled(),
                setting.isInquiryEnabled()
        );
    }
}
