package com.snow.popin.domain.mission.repository;

import com.snow.popin.domain.mission.entity.UserMission;
import com.snow.popin.domain.user.entity.User;

import java.util.Collection;
import java.util.List;

public interface MissionSetRepositoryCustom {
    int bulkEnableByPopupIds(Collection<Long> popupIds);
    int bulkDisableByPopupIds(Collection<Long> popupIds);
}
