package com.snow.popin.domain.popupstat.controller;

import com.snow.popin.domain.popupstat.dto.PopupStatsResponseDto;
import com.snow.popin.domain.popupstat.service.PopupStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/host/popups")
@RequiredArgsConstructor
public class PopupStatsController {

    private final PopupStatsService popupStatsService;

    /**
     * 특정 팝업의 통계를 조회한다.
     *
     * @param popupId 팝업 ID
     * @param start   조회 시작일
     * @param end     조회 종료일
     * @return PopupStatsResponseDto
     */
    @GetMapping("/{popupId}/stats")
    public ResponseEntity<List<PopupStatsResponseDto>> getPopupStats(
            @PathVariable Long popupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        log.info("[PopupStatsController] 팝업 통계 조회 요청: popupId={}, start={}, end={}", popupId, start, end);

        List<PopupStatsResponseDto> stats = popupStatsService.getStats(popupId, start, end);

        log.info("[PopupStatsController] 팝업 통계 조회 완료: popupId={}, count={}", popupId, stats.size());
        return ResponseEntity.ok(stats);
    }
}
