package com.snow.popin.domain.notification.dto.request;

import lombok.Getter;

@Getter
public class NotificationSettingRequestDto {
    private String type;   // "RESERVATION", "SYSTEM", "EVENT"
    private boolean enabled;
}
