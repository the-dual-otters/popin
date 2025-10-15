package com.snow.popin.domain.mission.repository;

import java.time.LocalDateTime;

public interface UserMissionRepositoryCustom {
    Long countCompletedMissionsByPopupAndDate(Long popupId, LocalDateTime start, LocalDateTime end);
}
