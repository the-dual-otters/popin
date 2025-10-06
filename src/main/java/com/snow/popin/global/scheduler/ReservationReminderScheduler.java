package com.snow.popin.global.scheduler;

import com.snow.popin.domain.notification.constant.NotificationType;
import com.snow.popin.domain.notification.service.NotificationService;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.snow.popin.domain.popupReservation.repository.ReservationQueryDslRepository;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationReminderScheduler {

    private final ReservationRepository reservationRepository;
    private final ReservationQueryDslRepository reservationQueryDslRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 매 분 0초마다 예약 임박 사용자에게 알림 발송
     */
    @Scheduled(cron = "0 * * * * *") // 매 분 0초 실행
    public void sendReservationReminders() {
        LocalDateTime now = LocalDateTime.now();

        // 예약 30분 전
        LocalDateTime target30m = now.plusMinutes(30);
        String target30mStr = target30m.format(FORMATTER);

        // 예약 하루 전
        LocalDateTime target1d = now.plusDays(1);
        String target1dStr = target1d.format(FORMATTER);

        // 30분 전 알림
        List<Reservation> reservations30m = reservationQueryDslRepository.findByReservationMinute(target30mStr);
        sendNotifications(reservations30m, "예약 임박", "30분 후 예약하신 일정이 시작됩니다.");

        // 하루 전 알림
        List<Reservation> reservations1d = reservationQueryDslRepository.findByReservationMinute(target1dStr);
        sendNotifications(reservations1d, "예약 하루 전", "예약하신 일정이 내일 시작됩니다.");
    }

    private void sendNotifications(List<Reservation> reservations, String title, String defaultMessage) {
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() != ReservationStatus.RESERVED) {
                continue; // RESERVED 상태만 알림 발송
            }

            Long userId = reservation.getUser().getId();
            String popupTitle = reservation.getPopup().getTitle(); // 팝업 이름
            String message = String.format("[%s] %s", popupTitle, defaultMessage);

            try {
                notificationService.createNotification(
                        userId,
                        title,
                        message,
                        NotificationType.RESERVATION,
                        "/users/user-popup-reservation"
                );
                log.info("알림 발송 완료 - type={}, userId={}, reservationId={}, popupTitle={}",
                        title, userId, reservation.getId(), popupTitle);
            } catch (Exception e) {
                log.error("알림 발송 실패 - type={}, userId={}, reservationId={}",
                        title, userId, reservation.getId(), e);
            }
        }
    }
}
