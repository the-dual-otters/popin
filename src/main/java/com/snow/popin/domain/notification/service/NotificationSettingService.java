package com.snow.popin.domain.notification.service;

import com.snow.popin.domain.notification.dto.response.NotificationSettingResponseDto;
import com.snow.popin.domain.notification.entity.NotificationSetting;
import com.snow.popin.domain.notification.constant.NotificationType;
import com.snow.popin.domain.notification.repository.NotificationSettingRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.exception.NotificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository settingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public NotificationSettingResponseDto getSettings(Long userId) {
        NotificationSetting setting = settingRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSetting(userId));
        return NotificationSettingResponseDto.from(setting);
    }

    @Transactional
    public void updateSetting(Long userId, String type, boolean enabled) {
        NotificationSetting notificationSetting = settingRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSetting(userId));

        NotificationType nType;
        try {
            nType = NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NotificationException.InvalidNotificationType(type);
        }


        switch (nType) {
            case RESERVATION:
                if (enabled) notificationSetting.enableReservation();
                else notificationSetting.disableReservation();
                break;

            case SYSTEM:
                if (enabled) notificationSetting.enableSystem();
                else notificationSetting.disableSystem();
                break;

            case EVENT:
                if (enabled) notificationSetting.enableInquiry();
                else notificationSetting.disableInquiry();
                break;
        }
        settingRepository.save(notificationSetting);
    }

    private NotificationSetting createDefaultSetting(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        NotificationSetting setting = NotificationSetting.createDefault(user);
        return settingRepository.save(setting);
    }
}
