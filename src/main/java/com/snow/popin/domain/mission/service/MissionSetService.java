package com.snow.popin.domain.mission.service;

import com.snow.popin.domain.mission.dto.response.MissionSetViewDto;
import com.snow.popin.domain.mission.dto.response.MissionSummaryDto;
import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.entity.UserMission;
import com.snow.popin.domain.mission.constant.UserMissionStatus;
import com.snow.popin.domain.mission.repository.MissionSetRepository;
import com.snow.popin.domain.mission.repository.UserMissionRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.global.exception.MissionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionSetService {

    private final MissionSetRepository missionSetRepository;
    private final UserMissionRepository userMissionRepository;
    private final PopupRepository popupRepository;

    public MissionSetViewDto getOne(UUID missionSetId, Long userId) {
        MissionSet ms = missionSetRepository.findById(missionSetId)
                .orElseThrow(MissionException.MissionSetNotFound::new);

        return toViewDto(ms, userId);
    }

    private MissionSetViewDto toViewDto(MissionSet set, Long userId) {
        if (set == null) {
            throw new MissionException.MissionSetNotFound();
        }
        if (set.isDisabled()) {
            throw new MissionException.MissionSetDisabled();
        }


        List<MissionSummaryDto> missions =
                Optional.ofNullable(set.getMissions()).orElse(Collections.emptyList())
                        .stream()
                        .map(m -> MissionSummaryDto.builder()
                                .id(m.getId())
                                .title(m.getTitle())
                                .description(m.getDescription())
                                .build())
                        .collect(Collectors.toList());

        long successCnt = 0L;
        Map<UUID, UserMissionStatus> statusByMission = new HashMap<>(10000);

        if (userId != null) {
            List<UserMission> userMissions = userMissionRepository
                    .findByUser_IdAndMission_MissionSet_Id(userId, set.getId());

            if (userMissions == null) {
                throw new MissionException.MissionNotFound();
            }

            for (UserMission userMission : userMissions) {
                if (userMission.getMission() != null) {
                    statusByMission.put(userMission.getMission().getId(), userMission.getStatus());
                }
                if (userMission.getStatus() == UserMissionStatus.COMPLETED) {
                    successCnt++;
                }
            }

            missions = missions.stream()
                    .map(ms -> MissionSummaryDto.builder()
                            .id(ms.getId())
                            .title(ms.getTitle())
                            .description(ms.getDescription())
                            .userStatus(statusByMission.get(ms.getId()))
                            .build())
                    .collect(Collectors.toList());
        }

        int req = (set.getRequiredCount() == null ? 0 : set.getRequiredCount());
        boolean cleared = successCnt >= req;

        Double latitude = null;
        Double longitude = null;
        if (set.getPopupId() != null) {
            Popup popup = popupRepository.findById(set.getPopupId())
                    .orElse(null);
            if (popup != null && popup.getVenue() != null) {
                latitude = popup.getVenue().getLatitude();
                longitude = popup.getVenue().getLongitude();
            }
        }

        return MissionSetViewDto.from(set, userId, missions, successCnt, cleared, latitude, longitude);
    }
}
