package com.snow.popin.domain.notification.controller;

import com.snow.popin.domain.notification.dto.request.NotificationSettingRequestDto;
import com.snow.popin.domain.notification.dto.response.NotificationSettingResponseDto;
import com.snow.popin.domain.notification.service.NotificationSettingService;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/settings")
@RequiredArgsConstructor
public class NotificationSettingApiController {

    private final NotificationSettingService settingService;
    private final UserUtil userUtil;

    /** 내 알림 설정 조회 */
    @GetMapping("/me")
    public ResponseEntity<NotificationSettingResponseDto> getMySettings() {
        Long userId = userUtil.getCurrentUserId();
        return ResponseEntity.ok(settingService.getSettings(userId));
    }

    /** 특정 타입 알림 설정 변경 */
    @PatchMapping("/{type}")
    public ResponseEntity<Void> updateSetting(
            @PathVariable String type,
            @RequestParam boolean enabled
    ) {
        Long userId = userUtil.getCurrentUserId();
        settingService.updateSetting(userId, type, enabled);
        return ResponseEntity.noContent().build(); // 204 응답
    }


}
