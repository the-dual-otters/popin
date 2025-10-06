package com.snow.popin.domain.review.controller;

import com.snow.popin.domain.review.dto.*;
import com.snow.popin.domain.review.service.ReviewService;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserUtil userUtil;

    // 리뷰 작성
    @PostMapping
    public ResponseEntity<ReviewResponseDto> createReview(
            @Valid @RequestBody ReviewCreateRequestDto request) {
        Long userId = userUtil.getCurrentUserId();
        ReviewResponseDto response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequestDto request) {
        Long userId = userUtil.getCurrentUserId();
        ReviewResponseDto response = reviewService.updateReview(reviewId, userId, request);
        return ResponseEntity.ok(response);
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        Long userId = userUtil.getCurrentUserId();
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    // 특정 팝업의 최근 리뷰 조회 (상세페이지용)
    @GetMapping("/popup/{popupId}/recent")
    public ResponseEntity<List<ReviewListResponseDto>> getRecentReviews(
            @PathVariable Long popupId,
            @RequestParam(defaultValue = "2")  @Min(1)  int limit) {
        List<ReviewListResponseDto> reviews = reviewService.getRecentReviewsByPopup(popupId, limit);
        return ResponseEntity.ok(reviews);
    }

    // 특정 팝업의 전체 리뷰 조회 (페이징)
    @GetMapping("/popup/{popupId}")
    public ResponseEntity<Page<ReviewListResponseDto>> getReviewsByPopup(
            @PathVariable Long popupId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReviewListResponseDto> reviews = reviewService.getReviewsByPopup(popupId, pageable);
        return ResponseEntity.ok(reviews);
    }

    // 팝업 리뷰 통계 조회
    @GetMapping("/popup/{popupId}/stats")
    public ResponseEntity<ReviewStatsDto> getReviewStats(@PathVariable Long popupId) {
        ReviewStatsDto stats = reviewService.getReviewStats(popupId);
        return ResponseEntity.ok(stats);
    }

    // 현재 사용자의 리뷰 목록 조회
    @GetMapping("/me")
    public ResponseEntity<Page<ReviewResponseDto>> getMyReviews(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = userUtil.getCurrentUserId();
        Page<ReviewResponseDto> reviews = reviewService.getReviewsByUser(userId, pageable);
        return ResponseEntity.ok(reviews);
    }

    // 리뷰 단건 조회
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponseDto> getReview(@PathVariable Long reviewId) {
        ReviewResponseDto review = reviewService.getReview(reviewId);
        return ResponseEntity.ok(review);
    }

    // 사용자가 특정 팝업에 리뷰 작성 여부 확인
    @GetMapping("/popup/{popupId}/check")
    public ResponseEntity<Map<String, Object>> checkUserReview(@PathVariable Long popupId) {
        Long userId = userUtil.getCurrentUserId();
        boolean hasReviewed = reviewService.hasUserReviewedPopup(popupId, userId);

        Map<String, Object> response = Map.of(
                "hasReviewed", hasReviewed,
                "popupId", popupId,
                "userId", userId
        );

        return ResponseEntity.ok(response);
    }

    // 사용자의 특정 팝업 리뷰 조회
    @GetMapping("/popup/{popupId}/me")
    public ResponseEntity<ReviewResponseDto> getMyReviewForPopup(@PathVariable Long popupId) {
        Long userId = userUtil.getCurrentUserId();
        ReviewResponseDto review = reviewService.getUserReviewForPopup(popupId, userId);
        return ResponseEntity.ok(review);
    }

    // 팝업별 리뷰 통계와 최근 리뷰 통합 조회 (상세페이지용)
    @GetMapping("/popup/{popupId}/summary")
    public ResponseEntity<Map<String, Object>> getPopupReviewSummary(@PathVariable Long popupId) {
        ReviewStatsDto stats = reviewService.getReviewStats(popupId);
        List<ReviewListResponseDto> recentReviews = reviewService.getRecentReviewsByPopup(popupId, 2);

        Map<String, Object> summary = Map.of(
                "stats", stats,
                "recentReviews", recentReviews,
                "hasMore", recentReviews.size() >= 2 && stats.getTotalReviews() > 2
        );

        return ResponseEntity.ok(summary);
    }
}