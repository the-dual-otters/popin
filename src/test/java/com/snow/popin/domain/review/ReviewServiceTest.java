package com.snow.popin.domain.review;

import com.snow.popin.domain.map.entity.Venue;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.review.dto.*;
import com.snow.popin.domain.review.entity.Review;
import com.snow.popin.domain.review.repository.ReviewRepository;
import com.snow.popin.domain.review.service.ReviewService;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.exception.ReviewException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("리뷰 서비스 테스트")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PopupRepository popupRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰 작성 성공")
    void createReview_Success() {
        // given
        Long userId = 1L;
        Long reviewId = 100L;
        ReviewCreateRequestDto requestDto = createReviewCreateRequestDto();
        User mockUser = createTestUser(userId);
        Popup mockPopup = createTestPopup(requestDto.getPopupId());

        Review mockReview = Review.of(
                requestDto.getPopupId(),
                userId,
                requestDto.getContent(),
                requestDto.getRating()
        );
        ReflectionTestUtils.setField(mockReview, "id", reviewId);

        given(popupRepository.findById(requestDto.getPopupId())).willReturn(Optional.of(mockPopup));
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(reviewRepository.save(any(Review.class))).willReturn(mockReview);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(mockReview));

        // when
        ReviewResponseDto result = reviewService.createReview(userId, requestDto);

        // then
        assertThat(result.getId()).isEqualTo(reviewId);
        assertThat(result.getContent()).isEqualTo(requestDto.getContent());
        assertThat(result.getRating()).isEqualTo(requestDto.getRating());
        assertThat(result.getPopupId()).isEqualTo(requestDto.getPopupId());
        assertThat(result.getUserId()).isEqualTo(userId);

        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).findById(reviewId);
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 존재하지 않는 팝업")
    void createReview_PopupNotFound() {
        // given
        Long userId = 1L;
        ReviewCreateRequestDto requestDto = createReviewCreateRequestDto();

        given(popupRepository.findById(requestDto.getPopupId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, requestDto))
                .isInstanceOf(ReviewException.PopupNotFound.class);

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 존재하지 않는 사용자")
    void createReview_UserNotFound() {
        // given
        Long userId = 1L;
        ReviewCreateRequestDto requestDto = createReviewCreateRequestDto();
        Popup mockPopup = createTestPopup(requestDto.getPopupId());

        given(popupRepository.findById(requestDto.getPopupId())).willReturn(Optional.of(mockPopup));
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, requestDto))
                .isInstanceOf(ReviewException.UserNotFound.class);

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 중복 리뷰")
    void createReview_DuplicateReview() {
        // given
        Long userId = 1L;
        ReviewCreateRequestDto requestDto = createReviewCreateRequestDto();
        User mockUser = createTestUser(userId);
        Popup mockPopup = createTestPopup(requestDto.getPopupId());

        given(popupRepository.findById(requestDto.getPopupId())).willReturn(Optional.of(mockPopup));
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));

        given(reviewRepository.save(any(Review.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(userId, requestDto))
                .isInstanceOf(ReviewException.DuplicateReview.class);

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_Success() {
        // given
        Long reviewId = 1L;
        Long userId = 1L;
        ReviewUpdateRequestDto requestDto = createReviewUpdateRequestDto();
        Review mockReview = createTestReview(reviewId, userId, 1L);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(mockReview));

        // when
        ReviewResponseDto result = reviewService.updateReview(reviewId, userId, requestDto);

        // then
        assertThat(result.getContent()).isEqualTo(requestDto.getContent());
        assertThat(result.getRating()).isEqualTo(requestDto.getRating());

        verify(reviewRepository).findById(reviewId);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 존재하지 않는 리뷰")
    void updateReview_ReviewNotFound() {
        // given
        Long reviewId = 999L;
        Long userId = 1L;
        ReviewUpdateRequestDto requestDto = createReviewUpdateRequestDto();

        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, userId, requestDto))
                .isInstanceOf(ReviewException.ReviewNotFound.class);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 권한 없음")
    void updateReview_AccessDenied() {
        // given
        Long reviewId = 1L;
        Long userId = 1L;
        Long otherUserId = 2L;
        ReviewUpdateRequestDto requestDto = createReviewUpdateRequestDto();
        Review mockReview = createTestReview(reviewId, otherUserId, 1L); // 다른 사용자의 리뷰

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(mockReview));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, userId, requestDto))
                .isInstanceOf(ReviewException.AccessDenied.class);
    }

    @Test
    @DisplayName("리뷰 수정 실패 - 차단된 리뷰")
    void updateReview_BlockedReview() {
        // given
        Long reviewId = 1L;
        Long userId = 1L;
        ReviewUpdateRequestDto requestDto = createReviewUpdateRequestDto();
        Review mockReview = createTestReview(reviewId, userId, 1L);
        mockReview.block(); // 리뷰 차단

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(mockReview));

        // when & then
        assertThatThrownBy(() -> reviewService.updateReview(reviewId, userId, requestDto))
                .isInstanceOf(ReviewException.BlockedReview.class);
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_Success() {
        // given
        Long reviewId = 1L;
        Long userId = 1L;
        Review mockReview = createTestReview(reviewId, userId, 1L);

        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(mockReview));
        willDoNothing().given(reviewRepository).delete(mockReview);

        // when
        reviewService.deleteReview(reviewId, userId);

        // then
        verify(reviewRepository).delete(mockReview);
    }

    @Test
    @DisplayName("팝업의 최근 리뷰 조회 성공")
    void getRecentReviewsByPopup_Success() {
        // given
        Long popupId = 1L;
        int limit = 2;
        List<Review> mockReviews = Arrays.asList(
                createTestReviewWithUser(1L, 1L, popupId),
                createTestReviewWithUser(2L, 2L, popupId)
        );

        given(reviewRepository.findTop10ByPopupIdAndIsBlockedFalseOrderByCreatedAtDesc(popupId))
                .willReturn(mockReviews);

        // when
        List<ReviewListResponseDto> result = reviewService.getRecentReviewsByPopup(popupId, limit);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("팝업의 전체 리뷰 조회 성공 - 페이징")
    void getReviewsByPopup_Success() {
        // given
        Long popupId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Review> mockReviews = Arrays.asList(
                createTestReviewWithUser(1L, 1L, popupId),
                createTestReviewWithUser(2L, 2L, popupId)
        );
        Page<Review> mockPage = new PageImpl<>(mockReviews, pageable, 2);

        given(reviewRepository.findByPopupIdAndIsBlockedFalse(popupId, pageable))
                .willReturn(mockPage);

        // when
        Page<ReviewListResponseDto> result = reviewService.getReviewsByPopup(popupId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자 리뷰 목록 조회 성공")
    void getReviewsByUser_Success() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<Review> mockReviews = Arrays.asList(
                createTestReviewWithPopup(1L, userId, 1L),
                createTestReviewWithPopup(2L, userId, 2L)
        );
        Page<Review> mockPage = new PageImpl<>(mockReviews, pageable, 2);

        given(reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                .willReturn(mockPage);

        // when
        Page<ReviewResponseDto> result = reviewService.getReviewsByUser(userId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("팝업 리뷰 통계 조회 성공")
    void getReviewStats_Success() {
        // given
        Long popupId = 1L;
        Object[] mockStats = {4.5, 10L}; // 평균 평점 4.5, 리뷰 수 10개

        given(reviewRepository.findRatingStatsByPopupId(popupId)).willReturn(mockStats);

        // when
        ReviewStatsDto result = reviewService.getReviewStats(popupId);

        // then
        assertThat(result.getAverageRating()).isEqualTo(4.5);
        assertThat(result.getTotalReviews()).isEqualTo(10L);
    }

    @Test
    @DisplayName("팝업 리뷰 통계 조회 - 리뷰가 없는 경우")
    void getReviewStats_NoReviews() {
        // given
        Long popupId = 1L;
        Object[] mockStats = {null, 0L};

        given(reviewRepository.findRatingStatsByPopupId(popupId)).willReturn(mockStats);

        // when
        ReviewStatsDto result = reviewService.getReviewStats(popupId);

        // then
        assertThat(result.getAverageRating()).isEqualTo(0.0);
        assertThat(result.getTotalReviews()).isEqualTo(0L);
    }

    @Test
    @DisplayName("사용자 리뷰 작성 여부 확인 - 작성함")
    void hasUserReviewedPopup_HasReviewed() {
        // given
        Long popupId = 1L;
        Long userId = 1L;

        given(reviewRepository.existsByPopupIdAndUserId(popupId, userId)).willReturn(true);

        // when
        boolean result = reviewService.hasUserReviewedPopup(popupId, userId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("사용자 리뷰 작성 여부 확인 - 작성 안함")
    void hasUserReviewedPopup_NotReviewed() {
        // given
        Long popupId = 1L;
        Long userId = 1L;

        given(reviewRepository.existsByPopupIdAndUserId(popupId, userId)).willReturn(false);

        // when
        boolean result = reviewService.hasUserReviewedPopup(popupId, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("사용자의 특정 팝업 리뷰 조회 성공")
    void getUserReviewForPopup_Success() {
        // given
        Long popupId = 1L;
        Long userId = 1L;
        Review mockReview = createTestReview(1L, userId, popupId);

        given(reviewRepository.findByPopupIdAndUserId(popupId, userId)).willReturn(Optional.of(mockReview));

        // when
        ReviewResponseDto result = reviewService.getUserReviewForPopup(popupId, userId);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getPopupId()).isEqualTo(popupId);
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("사용자의 특정 팝업 리뷰 조회 실패 - 리뷰 없음")
    void getUserReviewForPopup_ReviewNotFound() {
        // given
        Long popupId = 1L;
        Long userId = 1L;

        given(reviewRepository.findByPopupIdAndUserId(popupId, userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.getUserReviewForPopup(popupId, userId))
                .isInstanceOf(ReviewException.ReviewNotFound.class);
    }

    // Helper methods
    private ReviewCreateRequestDto createReviewCreateRequestDto() {
        return ReviewCreateRequestDto.builder()
                .popupId(1L)
                .content("정말 좋은 팝업이었습니다!")
                .rating(5)
                .build();
    }

    private ReviewUpdateRequestDto createReviewUpdateRequestDto() {
        return ReviewUpdateRequestDto.builder()
                .content("수정된 리뷰 내용입니다.")
                .rating(4)
                .build();
    }

    private User createTestUser(Long userId) {
        User user = User.builder()
                .email("test" + userId + "@example.com")
                .password("password")
                .name("Test User " + userId)
                .nickname("nick" + userId)
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private Popup createTestPopup(Long popupId) {
        Venue mockVenue = mock(Venue.class);
        Popup popup = Popup.createForTest("Test Popup " + popupId, PopupStatus.ONGOING, mockVenue);
        ReflectionTestUtils.setField(popup, "id", popupId);
        return popup;
    }

    private Review createTestReview(Long reviewId, Long userId, Long popupId) {
        Review review = Review.of(popupId, userId, "테스트 리뷰 내용", 4);
        ReflectionTestUtils.setField(review, "id", reviewId);
        ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(review, "updatedAt", LocalDateTime.now());
        return review;
    }

    private Review createTestReviewWithUser(Long reviewId, Long userId, Long popupId) {
        Review review = createTestReview(reviewId, userId, popupId);
        User user = createTestUser(userId);
        ReflectionTestUtils.setField(review, "user", user);
        return review;
    }

    private Review createTestReviewWithPopup(Long reviewId, Long userId, Long popupId) {
        Review review = createTestReview(reviewId, userId, popupId);
        Popup popup = createTestPopup(popupId);
        ReflectionTestUtils.setField(review, "popup", popup);
        return review;
    }
}