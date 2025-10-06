package com.snow.popin.domain.popup.service;

import com.snow.popin.domain.mission.repository.MissionRepository;
import com.snow.popin.domain.mission.repository.MissionSetRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupQueryDslRepository;
import com.snow.popin.domain.popup.repository.PopupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopupBatchService {

    private final PopupRepository popupRepository;
    private final PopupQueryDslRepository popupQueryDslRepository;
    private final MissionSetRepository missionSetRepository;

    //매일 자정, 팝업의 상태를 자동으로 업데이트합니다.
    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void updatePopupStatuses() {
        log.info("팝업 상태 업데이트 스케줄러 시작");
        LocalDate today = LocalDate.now();
        int updatedCount = 0;

        // PLANNED -> ONGOING 업데이트
        List<Popup> popupsToStart = popupQueryDslRepository.findPopupsToUpdateToOngoing(today);
        for (Popup popup : popupsToStart) {
            if (popup.updateStatus()) {
                updatedCount++;
            }
        }
        if (!popupsToStart.isEmpty()) {
            var ids = popupsToStart.stream().map(Popup::getId).collect(Collectors.toSet());
            int enabled = missionSetRepository.bulkEnableByPopupIds(ids);
            log.info("ONGOING 전환된 팝업 {}건에 대해 미션셋 {}건 ENABLE(벌크) 처리", popupsToStart.size(), enabled);
            popupRepository.saveAll(popupsToStart);
        }

        // ONGOING -> ENDED 업데이트
        List<Popup> popupsToEnd = popupQueryDslRepository.findPopupsToUpdateToEnded(today);
        for (Popup popup : popupsToEnd) {
            if (popup.updateStatus()) {
                updatedCount++;
            }
        }
        if (!popupsToEnd.isEmpty()) {
            var ids = popupsToEnd.stream().map(Popup::getId).collect(Collectors.toSet());
            int disabled = missionSetRepository.bulkDisableByPopupIds(ids);
            log.info("ENDED 전환된 팝업 {}건에 대해 미션셋 {}건 DISABLE(벌크) 처리", popupsToEnd.size(), disabled);
            popupRepository.saveAll(popupsToEnd);
        }

        if (updatedCount > 0) {
            log.info("총 {}개의 팝업 상태가 업데이트되었습니다.", updatedCount);
        } else {
            log.info("상태를 업데이트할 팝업이 없습니다.");
        }
        log.info("팝업 상태 업데이트 스케줄러 종료");
    }
}