package com.snow.popin.domain.popup.controller;

import com.snow.popin.domain.popup.dto.response.PopupDetailResponseDto;
import com.snow.popin.domain.popup.dto.response.PopupListResponseDto;
import com.snow.popin.domain.popup.dto.response.PopupSummaryResponseDto;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.service.PopupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/popups")
@RequiredArgsConstructor
public class PopupController {

    private final PopupService popupService;

    // ===== 메인 페이지 필터링 API =====

    // 전체 팝업 조회
    @GetMapping
    public ResponseEntity<PopupListResponseDto> getAllPopups(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("전체 팝업 조회 API 호출 - status: {}, page: {}, size: {}", status, page, size);

        PopupStatus popupStatus = popupService.parseStatus(status);
        PopupListResponseDto response = popupService.getAllPopups(page, size, popupStatus);

        return ResponseEntity.ok(response);
    }

    // 인기 팝업 조회
    @GetMapping("/popular")
    public ResponseEntity<PopupListResponseDto> getPopularPopups(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("인기 팝업 조회 API 호출 - status: {}, page: {}, size: {}", status, page, size);

        PopupListResponseDto response = popupService.getPopularPopups(page, size);

        return ResponseEntity.ok(response);
    }

    // 마감임박 팝업 조회
    @GetMapping("/deadline")
    public ResponseEntity<PopupListResponseDto> getDeadlineSoonPopups(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("마감임박 팝업 조회 API 호출 - status: {}, page: {}, size: {}", status, page, size);

        PopupStatus popupStatus = popupService.parseStatus(status);
        PopupListResponseDto response = popupService.getDeadlineSoonPopups(page, size, popupStatus);

        return ResponseEntity.ok(response);
    }

    // 지역별 + 날짜별 팝업 조회
    @GetMapping("/region-date")
    public ResponseEntity<PopupListResponseDto> getPopupsByRegionAndDate(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("지역별 날짜별 팝업 조회 API 호출 - region: {}, dateFilter: {}, startDate: {}, endDate: {}",
                region, dateFilter, startDate, endDate);

        PopupListResponseDto response = popupService.getPopupsByRegionAndDate(
                region, dateFilter, startDate, endDate, page, size);

        return ResponseEntity.ok(response);
    }

    // AI 추천 팝업 조회
    @GetMapping("/ai-recommended")
    public ResponseEntity<PopupListResponseDto> getAIRecommendedPopups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("AI 추천 팝업 조회 - page: {}, size: {}", page, size);

        // 페이지/사이즈 검증
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 10;

        PopupListResponseDto result = popupService.getAIRecommendedPopups(page, size);

        log.info("AI 추천 팝업 조회 완료 - 총 {}개", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    // ===== 팝업 상세 조회 API =====

    // 팝업 상세 조회 (조회수 증가)
    @GetMapping("/{id}")
    public ResponseEntity<PopupDetailResponseDto> getPopupDetail(@PathVariable Long id) {
        log.info("팝업 상세 조회 API 호출 - popupId: {}", id);

        PopupDetailResponseDto response = popupService.getPopupDetail(id);
        return ResponseEntity.ok(response);
    }

    // 팝업 상세 조회 (조회수 증가 없음 - 관리자용)
    @GetMapping("/{id}/admin")
    public ResponseEntity<PopupDetailResponseDto> getPopupDetailForAdmin(@PathVariable Long id) {
        log.info("팝업 상세 조회 (관리자) API 호출 - popupId: {}", id);

        PopupDetailResponseDto response = popupService.getPopupDetailForAdmin(id);
        return ResponseEntity.ok(response);
    }

    // 유사한 팝업 조회 (같은 카테고리)
    @GetMapping("/{id}/similar")
    public ResponseEntity<PopupListResponseDto> getSimilarPopups(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {

        log.info("유사 팝업 조회 API 호출 - popupId: {}, page: {}, size: {}", id, page, size);

        try {
            // 조회수 증가 없이 카테고리 정보만 가져오기
            PopupDetailResponseDto currentPopup = popupService.getPopupDetailForAdmin(id);

            if (currentPopup.getCategoryName() == null) {
                log.warn("팝업 ID {}의 카테고리 정보가 없어서 빈 결과 반환", id);
                return ResponseEntity.ok(PopupListResponseDto.empty(page, size));
            }

            PopupListResponseDto response = popupService.getSimilarPopups(
                    currentPopup.getCategoryName(), id, page, size);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("유사 팝업 조회 실패 - popupId: {}", id, e);
            return ResponseEntity.ok(PopupListResponseDto.empty(page, size));
        }
    }

    // ===== 카테고리 및 지역별 조회 API =====

    // 카테고리별 팝업 조회
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<PopupListResponseDto> getPopupsByCategory(
            @PathVariable String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("카테고리별 팝업 조회 API 호출 - 카테고리: {}, 페이지: {}, 크기: {}", categoryName, page, size);

        PopupListResponseDto response = popupService.getPopupsByCategory(categoryName, page, size);
        return ResponseEntity.ok(response);
    }

    // 지역별 팝업 조회
    @GetMapping("/region/{region}")
    public ResponseEntity<List<PopupSummaryResponseDto>> getPopupsByRegion(@PathVariable String region) {
        log.info("지역별 팝업 조회 API 호출 - 지역: {}", region);

        List<PopupSummaryResponseDto> popups = popupService.getPopupsByRegion(region);
        return ResponseEntity.ok(popups);
    }
}