package com.snow.popin.domain.notification.repository;

import com.snow.popin.domain.notification.entity.NotificationSetting;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    // 유저별 알림 설정 조회
    Optional<NotificationSetting> findByUser(User user);

    // userId로 바로 조회하고 싶을 때
    Optional<NotificationSetting> findByUserId(Long userId);
}
