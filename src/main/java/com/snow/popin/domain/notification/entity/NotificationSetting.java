package com.snow.popin.domain.notification.entity;

import com.snow.popin.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "notification_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 설정 대상 유저
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 전체 알림 ON/OFF
    @Column(nullable = false)
    private boolean enabled;

    // 예약 관련 알림
    @Column(nullable = false)
    private boolean reservationEnabled;

    // 문의 관련 알림
    @Column(nullable = false)
    private boolean inquiryEnabled;

    // 시스템 알림
    @Column(nullable = false)
    private boolean systemEnabled;

    private NotificationSetting(User user) {
        this.user = user;
        this.enabled = true;
        this.reservationEnabled = true;
        this.systemEnabled = true;
        this.inquiryEnabled = true;
    }

    public static NotificationSetting createDefault(User user) {
        return new NotificationSetting(user);
    }

    /**
     * 비즈니스 메소드
     */
    public void enableAll() { this.enabled = true; }
    public void disableAll() { this.enabled = false; }
    public void enableReservation() { this.reservationEnabled = true; }
    public void disableReservation() { this.reservationEnabled = false; }
    public void enableSystem() { this.systemEnabled = true; }
    public void disableSystem() { this.systemEnabled = false; }
    public void enableInquiry() { this.inquiryEnabled = true; }
    public void disableInquiry() { this.inquiryEnabled = false; }
}
