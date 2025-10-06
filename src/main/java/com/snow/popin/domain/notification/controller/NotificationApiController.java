package com.snow.popin.domain.notification.controller;

import com.snow.popin.domain.notification.dto.response.NotificationResponseDto;
import com.snow.popin.domain.notification.entity.Notification;
import com.snow.popin.domain.notification.constant.NotificationType;
import com.snow.popin.domain.notification.service.NotificationService;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserUtil userUtil;

    // 유저별 SSE 연결 보관
    private static final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** 내 알림 목록 조회 */
    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponseDto>> getMyNotifications() {
        Long userId = userUtil.getCurrentUserId();

        List<Notification> list = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(
                list.stream()
                        .map(NotificationResponseDto::from)
                        .collect(Collectors.toList())
        );
    }

    /** 알림 읽음 처리 */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /** 예약 확정 시 알림 (테스트용 엔드포인트) */
    @PostMapping("/reservation")
    public ResponseEntity<Void> sendReservationNotification(@RequestParam Long userId) {
        Notification n = notificationService.createNotification(
                userId,
                "예약 확정",
                "예약이 확정되었습니다!",
                NotificationType.RESERVATION,
                "/users/user-popup-reservation"
        );

        if (n != null) {
            sendToClient(userId, NotificationResponseDto.from(n));
        }

        return ResponseEntity.ok().build();
    }

    /** 특정 유저에게 SSE 전송 */
    public static void sendToClient(Long userId, NotificationResponseDto dto) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(dto));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
}
