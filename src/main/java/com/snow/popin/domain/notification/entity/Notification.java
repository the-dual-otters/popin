package com.snow.popin.domain.notification.entity;

import com.snow.popin.domain.notification.constant.NotificationType;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.common.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;


@Entity
@Getter
@Table(name = "notifications")
@NoArgsConstructor
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;   // 알림 대상 유저

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type; // RESERVATION, SYSTEM 등

    @Column(name="is_read")
    private boolean read = false;

    private String title;


    @Column(length = 500)
    private String link;

    @Builder
    public Notification(User user, String message, NotificationType type, String title, String link) {
        this.user = user;
        this.message = message;
        this.type = type;
        this.title = title;
        this.read = false;
        this.link = link;
    }

    public void markAsRead() {
        this.read = true;
    }

}
