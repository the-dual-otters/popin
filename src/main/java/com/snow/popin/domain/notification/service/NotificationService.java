package com.snow.popin.domain.notification.service;

import com.snow.popin.domain.notification.controller.NotificationApiController;
import com.snow.popin.domain.notification.dto.response.NotificationResponseDto;
import com.snow.popin.domain.notification.entity.Notification;
import com.snow.popin.domain.notification.entity.NotificationSetting;
import com.snow.popin.domain.notification.constant.NotificationType;
import com.snow.popin.domain.notification.repository.NotificationRepository;
import com.snow.popin.domain.notification.repository.NotificationSettingRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    /**
     * 특정 사용자에게 알림 생성 & SSE 푸시
     */
    @Transactional
    public Notification createNotification(Long userId, String title, String message, NotificationType type, String link) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 유저 알림 설정 확인
        NotificationSetting setting = notificationSettingRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("알림 설정이 없습니다."));

        // 전체 OFF
        if (!setting.isEnabled()) {
            return null;
        }

        // 타입별 OFF
        switch (type) {
            case RESERVATION:
                if (!setting.isReservationEnabled()) return null;
                break;
            case SYSTEM:
                if (!setting.isSystemEnabled()) return null;
                break;
            case EVENT:
                if (!setting.isInquiryEnabled()) return null;
                break;
        }

        // 알림 저장
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .link(link)
                .build();

        Notification saved = notificationRepository.save(notification);

        // SSE 푸시
        NotificationApiController.sendToClient(userId, NotificationResponseDto.from(saved));

        return saved;
    }

    /**
     * 유저 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다. id=" + userId));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));
        n.markAsRead();
    }
}
