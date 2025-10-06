package com.snow.popin.domain.review.service;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.review.dto.*;
import com.snow.popin.domain.review.entity.Review;
import com.snow.popin.domain.review.repository.ReviewRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.exception.ReviewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PopupRepository popupRepository;
    private final UserRepository userRepository;

    /**
     * 리뷰 작성
     */
    @Transactional
    public ReviewResponseDto createReview(Long userId, ReviewCreateRequestDto request) {
        // 팝업 존재 확인
        Popup popup = popupRepository.findById(request.getPopupId())
                .orElseThrow(() -> new ReviewException.PopupNotFound(request.getPopupId()));

        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ReviewException.UserNotFound(userId));

        // 리뷰 생성 및 저장 (DB 유니크로 보강)
        Review review = request.toEntity(userId);
        Review savedReview;
        try {
            savedReview = reviewRepository.save(review);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ReviewException.DuplicateReview(request.getPopupId());
        }

        // 연관관계 설정 (조회용)
        savedReview = reviewRepository.findById(savedReview.getId()).orElseThrow();

        log.info("리뷰 작성 완료 - 사용자: {}, 팝업: {}, 평점: {}",
                user.getName(), popup.getTitle(), request.getRating());

        return ReviewResponseDto.from(savedReview);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public ReviewResponseDto updateReview(Long reviewId, Long userId, ReviewUpdateRequestDto request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException.ReviewNotFound(reviewId));

        // 수정 권한 확인
        if (!review.canEdit(userId)) {
            if (review.isBlocked()) {
                throw new ReviewException.BlockedReview();
            } else {
                throw new ReviewException.AccessDenied();
            }
        }

        // 내용 수정
        review.updateContent(request.getContent());
        review.updateRating(request.getRating());

        log.info("리뷰 수정 완료 - 리뷰ID: {}, 사용자ID: {}", reviewId, userId);

        return ReviewResponseDto.from(review);
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException.ReviewNotFound(reviewId));

        // 삭제 권한 확인
        if (!review.canEdit(userId)) {
            if (review.isBlocked()) {
                throw new ReviewException.BlockedReview();
            } else {
                throw new ReviewException.AccessDenied();
            }
        }

        reviewRepository.delete(review);
        log.info("리뷰 삭제 완료 - 리뷰ID: {}, 사용자ID: {}", reviewId, userId);
    }

    /**
     * 팝업의 최근 리뷰 조회 (상세페이지용 - 최대 2개)
     */
    @Transactional(readOnly = true)
    public List<ReviewListResponseDto> getRecentReviewsByPopup(Long popupId, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be >= 1");
        }
        if (limit > 100) { // hard cap
            limit = 100;
        }

        List<Review> reviews;
        if (limit <= 10) {
            // 10개 이하면 findTop10 메서드 사용
            reviews = reviewRepository.findTop10ByPopupIdAndIsBlockedFalseOrderByCreatedAtDesc(popupId);
            // 필요한 만큼만 자르기
            if (reviews.size() > limit) {
                reviews = reviews.subList(0, limit);
            }
        } else {
            // 10개 초과면 Pageable 사용
            Pageable pageable = PageRequest.of(0, limit);
            Page<Review> reviewPage = reviewRepository.findByPopupIdAndIsBlockedFalse(popupId, pageable);
            reviews = reviewPage.getContent();
        }

        return reviews.stream()
                .map(ReviewListResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 팝업의 전체 리뷰 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<ReviewListResponseDto> getReviewsByPopup(Long popupId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByPopupIdAndIsBlockedFalse(popupId, pageable);

        return reviewPage.map(ReviewListResponseDto::from);
    }

    /**
     * 사용자 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsByUser(Long userId, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return reviewPage.map(ReviewResponseDto::from);
    }

    /**
     * 팝업 리뷰 통계 조회
     */
    @Transactional(readOnly = true)
    public ReviewStatsDto getReviewStats(Long popupId) {
        log.info("리뷰 통계 조회 - 팝업ID: {}", popupId);

        long actualCount = reviewRepository.countByPopupIdAndIsBlockedFalse(popupId);
        log.info("실제 리뷰 개수: {}", actualCount);

        Object[] result = reviewRepository.findRatingStatsByPopupId(popupId);

        // 상세 디버깅
        log.info("result == null? {}", result == null);
        if (result != null) {
            log.info("[리뷰] result.length: {}", result.length);
            for (int i = 0; i < result.length; i++) {
                log.info("[리뷰]  result[{}]: {} (type: {})", i, result[i],
                        result[i] != null ? result[i].getClass().getSimpleName() : "null");
            }
        }

        ReviewStatsDto statsDto = ReviewStatsDto.from(result);
        log.info("최종 DTO: averageRating={}, totalReviews={}",
                statsDto.getAverageRating(), statsDto.getTotalReviews());

        return statsDto;
    }
    /**
     * 사용자가 해당 팝업에 리뷰 작성 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasUserReviewedPopup(Long popupId, Long userId) {
        return reviewRepository.existsByPopupIdAndUserId(popupId, userId);
    }

    /**
     * 사용자의 특정 팝업 리뷰 조회
     */
    @Transactional(readOnly = true)
    public ReviewResponseDto getUserReviewForPopup(Long popupId, Long userId) {
        Review review = reviewRepository.findByPopupIdAndUserId(popupId, userId)
                .orElseThrow(() -> new ReviewException.ReviewNotFound(popupId));

        return ReviewResponseDto.from(review);
    }

    /**
     * 리뷰 단건 조회
     */
    @Transactional(readOnly = true)
    public ReviewResponseDto getReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException.ReviewNotFound(reviewId));

        return ReviewResponseDto.from(review);
    }
}