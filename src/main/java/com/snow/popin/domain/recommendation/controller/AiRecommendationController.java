package com.snow.popin.domain.recommendation.controller;

import com.snow.popin.domain.popup.dto.response.PopupListResponseDto;
import com.snow.popin.domain.popup.service.PopupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 추천 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRecommendationController {

    private final PopupService popupService;

    /**
     * AI 추천 팝업 조회
     * - 로그인한 경우: 개인화 추천
     * - 로그인하지 않은 경우: 인기 팝업
     */
    @GetMapping("/recommendations")
    public ResponseEntity<PopupListResponseDto> getRecommendations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("AI 추천 팝업 조회 요청 - page: {}, size: {}", page, size);

        try {
            // 페이지/사이즈 검증
            if (page < 0) page = 0;
            if (size <= 0 || size > 50) size = 10;

            PopupListResponseDto recommendations = popupService.getAIRecommendedPopups(page, size);

            log.info("AI 추천 팝업 조회 완료 - 총 {}개", recommendations.getTotalElements());
            return ResponseEntity.ok(recommendations);

        } catch (Exception e) {
            log.error("AI 추천 팝업 조회 중 오류 발생", e);

            // 오류 발생 시 인기 팝업으로 대체
            PopupListResponseDto fallback = popupService.getPopularPopups(page, size);
            return ResponseEntity.ok(fallback);
        }
    }
}
