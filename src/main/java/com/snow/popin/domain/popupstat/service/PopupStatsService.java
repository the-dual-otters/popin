package com.snow.popin.domain.popupstat.service;

import com.snow.popin.domain.mission.repository.UserMissionRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.snow.popin.domain.popupReservation.repository.ReservationQueryDslRepository;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import com.snow.popin.domain.popupstat.dto.PopupStatsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 팝업에 대한 다양한 통계 데이터를 계산한다.
 * - 일별 예약/방문자/취소/미션 수행자 수
 * - 시간대별 방문자 수
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopupStatsService {

    private final PopupRepository popupRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationQueryDslRepository reservationQueryDslRepository;
    private final UserMissionRepository userMissionRepository;

    /**
     * 특정 팝업에 대한 통계를 조회.
     * 7일을 기본으로 함.
     *
     * @param popupId 팝업 ID
     * @param start   조회 시작일
     * @param end     조회 종료일
     * @return 통계 데이터 리스트 (일별 + 시간대별)
     */
    public List<PopupStatsResponseDto> getStats(Long popupId, LocalDate start, LocalDate end) {
        log.info("[PopupStatsService] 팝업 통계 조회 요청: popupId={}, start={}, end={}", popupId, start, end);

        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> {
                    log.error("[PopupStatsService] 팝업 없음: popupId={}", popupId);
                    return new IllegalArgumentException("존재하지 않는 팝업입니다.");
                });

        LocalDate startDate = start != null ? start : LocalDate.now().minusDays(6);
        LocalDate endDate = end != null ? end : LocalDate.now();

        List<PopupStatsResponseDto> result = new ArrayList<>();
        result.addAll(calculateDailyStats(popup, startDate, endDate));
        result.addAll(calculateHourlyStats(popup));

        log.info("[PopupStatsService] 팝업 통계 조회 완료: popupId={}, 일별+시간대 통계 count={}", popupId, result.size());
        return result;
    }

    /**
     * 일별 통계를 계산.
     *
     * @param popup     대상 팝업
     * @param startDate 시작일
     * @param endDate   종료일
     * @return 일별 통계 리스트
     */
    private List<PopupStatsResponseDto> calculateDailyStats(Popup popup, LocalDate startDate, LocalDate endDate) {
        log.debug("[PopupStatsService] 일별 통계 계산 시작: popupId={}, startDate={}, endDate={}", popup.getId(), startDate, endDate);

        List<PopupStatsResponseDto> dailyStats = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(23, 59, 59);

            Long reservationCount = reservationQueryDslRepository.countByPopupAndReservedAtBetweenAndStatus(
                    popup, dayStart, dayEnd, ReservationStatus.RESERVED
            );
            Long canceledCount = reservationQueryDslRepository.countByPopupAndReservedAtBetweenAndStatus(
                    popup, dayStart, dayEnd, ReservationStatus.CANCELLED
            );
            Long visitorCount = reservationQueryDslRepository.countByPopupAndReservedAtBetweenAndStatus(
                    popup, dayStart, dayEnd, ReservationStatus.VISITED
            );
            Long missionCount = userMissionRepository.countCompletedMissionsByPopupAndDate(
                    popup.getId(), dayStart, dayEnd
            );

            dailyStats.add(PopupStatsResponseDto.builder()
                    .date(date)
                    .hour(null)
                    .visitorCount(visitorCount.intValue())
                    .reservationCount(reservationCount.intValue())
                    .canceledCount(canceledCount.intValue())
                    .missionCompletedCount(missionCount.intValue())
                    .build());
        }

        log.debug("[PopupStatsService] 일별 통계 계산 완료: popupId={}, count={}", popup.getId(), dailyStats.size());
        return dailyStats;
    }

    /**
     * 시간대별 방문자 통계를 계산.
     * 예약의 reservedAt 사용.
     *
     * @param popup 대상 팝업
     * @return 시간대별 통계 리스트
     */
    private List<PopupStatsResponseDto> calculateHourlyStats(Popup popup) {
        log.debug("[PopupStatsService] 시간대별 통계 계산 시작: popupId={}", popup.getId());

        List<PopupStatsResponseDto> hourlyStats = new ArrayList<>();
        Map<Integer, Integer> hourlyVisitorMap = new HashMap<>();

        List<Reservation> allReservations = reservationRepository.findByPopupAndStatus(popup, ReservationStatus.VISITED);

        for (Reservation reservation : allReservations) {
            if (reservation.getReservedAt() != null) {
                int hour = reservation.getReservedAt().getHour();
                hourlyVisitorMap.put(hour, hourlyVisitorMap.getOrDefault(hour, 0) + 1);
            }
        }

        for (Map.Entry<Integer, Integer> entry : hourlyVisitorMap.entrySet()) {
            hourlyStats.add(PopupStatsResponseDto.builder()
                    .date(LocalDate.now())
                    .hour(entry.getKey())
                    .visitorCount(entry.getValue())
                    .reservationCount(0)
                    .canceledCount(0)
                    .missionCompletedCount(0)
                    .build());
        }

        log.debug("[PopupStatsService] 시간대별 통계 계산 완료: popupId={}, count={}", popup.getId(), hourlyStats.size());
        return hourlyStats;
    }
}
