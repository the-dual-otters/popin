package com.snow.popin.domain.space.scheduler;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpaceScheduler {

    private final SpaceRepository spaceRepository;

    // 매 시 정각 실행
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void hideExpiredSpaces() {
        LocalDate today = LocalDate.now();

        List<Space> expiredSpaces = spaceRepository.findAll().stream()
                .filter(space -> !space.getIsHidden()
                        && space.getEndDate() != null
                        && space.getEndDate().isBefore(today))
                .collect(Collectors.toList());

        expiredSpaces.forEach(Space::hide);

        log.info("만료된 공간 {}개가 숨김 처리됨", expiredSpaces.size());
    }
}
