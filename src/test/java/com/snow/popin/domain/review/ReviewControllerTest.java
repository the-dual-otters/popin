package com.snow.popin.domain.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snow.popin.domain.review.controller.ReviewController;
import com.snow.popin.domain.review.dto.*;
import com.snow.popin.domain.review.service.ReviewService;
import com.snow.popin.global.exception.ReviewException;
import com.snow.popin.global.util.UserUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReviewController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@DisplayName("리뷰 컨트롤러 테스트")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private UserUtil userUtil;

    @MockBean(name = "jwtUtil")
    private Object jwtUtil;

    @MockBean(name = "jwtFilter")
    private Object jwtFilter;

    @Test
    @DisplayName("리뷰 작성 성공")
    void createReview_Success() throws Exception {
        // given
        Long userId = 1L;
        ReviewCreateRequestDto requestDto = createReviewCreateRequestDto();
        ReviewResponseDto responseDto = ReviewResponseDto.builder()
                .id(1L)
                .popupId(requestDto.getPopupId())
                .popupTitle("Test Popup")
                .userId(userId)
                .userName("Test User")
                .userNickname("testnick")
                .content(requestDto.getContent())
                .rating(requestDto.getRating())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isBlocked(false)
                .build();

        given(userUtil.getCurrentUserId()).willReturn(userId);
        given(reviewService.createReview(eq(userId), any(ReviewCreateRequestDto.class)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.popupId").value(requestDto.getPopupId()))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.content").value(requestDto.getContent()))
                .andExpect(jsonPath("$.rating").value(requestDto.getRating()));
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 유효성 검사 오류")
    void createReview_ValidationError() throws Exception {
        // given
        ReviewCreateRequestDto invalidDto = new ReviewCreateRequestDto();
        // 필수 필드들이 비어있어 유효성 검사 실패

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 내용 길이 부족")
    void createReview_ContentTooShort() throws Exception {
        // given
        ReviewCreateRequestDto requestDto = ReviewCreateRequestDto.builder()
                .popupId(1L)
                .content("짧음") // 10자 미만
                .rating(5)
                .build();

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리뷰 작성 실패 - 잘못된 평점")
    void createReview_InvalidRating() throws Exception {
        // given
        ReviewCreateRequestDto requestDto = ReviewCreateRequestDto.builder()
                .popupId(1L)
                .content("정말 좋은 팝업이었습니다!")
                .rating(6) // 5점 초과
                .build();

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReview_Success() throws Exception {
        // given
        Long reviewId = 1L;
        Long userId = 1L;
        ReviewUpdateRequestDto requestDto = createReviewUpdateRequestDto();
        ReviewResponseDto responseDto = ReviewResponseDto.builder()
                .id(reviewId)
                .popupId(1L)
                .popupTitle("Test Popup")
                .userId(userId)
                .userName("Test User")
                .userNickname("testnick")
                .content(requestDto.getContent()) // 수정된 내용 사용
                .rating(requestDto.getRating())   // 수정된 평점 사용
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isBlocked(false)
                .build();

        given(userUtil.getCurrentUserId()).willReturn(userId);
        given(reviewService.updateReview(eq(reviewId), eq(userId), any(ReviewUpdateRequestDto.class)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(put("/api/reviews/{reviewId}", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.content").value(requestDto.getContent()))
                .andExpect(jsonPath("$.rating").value(requestDto.getRating()));
    }

    @Test
    @DisplayName("리뷰 삭제 성공")
    void deleteReview_Success() throws Exception {
        // given
        Long reviewId = 1L;
        Long userId = 1L;

        given(userUtil.getCurrentUserId()).willReturn(userId);
        willDoNothing().given(reviewService).deleteReview(reviewId, userId);

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("리뷰 삭제 실패 - 권한 없음")
    void deleteReview_AccessDenied() throws Exception {
        // given
        Long reviewId = 1L;
        Long userId = 1L;

        given(userUtil.getCurrentUserId()).willReturn(userId);
        willThrow(new ReviewException.AccessDenied()).given(reviewService).deleteReview(reviewId, userId);

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId))
                .andExpect(status().is5xxServerError())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(ReviewException.AccessDenied.class));
    }

    @Test
    @DisplayName("팝업의 최근 리뷰 조회 성공")
    void getRecentReviews_Success() throws Exception {
        // given
        Long popupId = 1L;
        List<ReviewListResponseDto> reviews = Arrays.asList(
                createReviewListResponseDto(1L, 1L),
                createReviewListResponseDto(2L, 2L)
        );

        given(reviewService.getRecentReviewsByPopup(popupId, 2)).willReturn(reviews);

        // when & then
        mockMvc.perform(get("/api/reviews/popup/{popupId}/recent", popupId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    @DisplayName("팝업의 최근 리뷰 조회 - 개수 제한")
    void getRecentReviews_WithLimit() throws Exception {
        // given
        Long popupId = 1L;
        int limit = 5;
        List<ReviewListResponseDto> reviews = Arrays.asList(
                createReviewListResponseDto(1L, 1L)
        );

        given(reviewService.getRecentReviewsByPopup(popupId, limit)).willReturn(reviews);

        // when & then
        mockMvc.perform(get("/api/reviews/popup/{popupId}/recent", popupId)
                        .param("limit", String.valueOf(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("팝업의 전체 리뷰 조회 - 페이징")
    void getReviewsByPopup_Success() throws Exception {
        // given
        Long popupId = 1L;
        List<ReviewListResponseDto> reviews = Arrays.asList(
                createReviewListResponseDto(1L, 1L),
                createReviewListResponseDto(2L, 2L)
        );
        Page<ReviewListResponseDto> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 10), 2);

        given(reviewService.getReviewsByPopup(eq(popupId), any(Pageable.class))).willReturn(reviewPage);

        // when & then
        mockMvc.perform(get("/api/reviews/popup/{popupId}", popupId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("팝업 리뷰 통계 조회 성공")
    void getReviewStats_Success() throws Exception {
        // given
        Long popupId = 1L;
        ReviewStatsDto statsDto = ReviewStatsDto.builder()
                .averageRating(4.5)
                .totalReviews(10L)
                .build();

        given(reviewService.getReviewStats(popupId)).willReturn(statsDto);

        // when & then
        mockMvc.perform(get("/api/reviews/popup/{popupId}/stats", popupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(10));
    }

    @Test
    @DisplayName("내 리뷰 목록 조회 성공")
    void getMyReviews_Success() throws Exception {
        // given
        Long userId = 1L;
        List<ReviewResponseDto> reviews = Arrays.asList(
                createReviewResponseDto(1L, userId, 1L),
                createReviewResponseDto(2L, userId, 2L)
        );
        Page<ReviewResponseDto> reviewPage = new PageImpl<>(reviews, PageRequest.of(0, 10), 2);

        given(userUtil.getCurrentUserId()).willReturn(userId);
        given(reviewService.getReviewsByUser(eq(userId), any(Pageable.class))).willReturn(reviewPage);

        // when & then
        mockMvc.perform(get("/api/reviews/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].userId").value(userId))
                .andExpect(jsonPath("$.content[1].userId").value(userId));
    }

    @Test
    @DisplayName("리뷰 단건 조회 성공")
    void getReview_Success() throws Exception {
        // given
        Long reviewId = 1L;
        ReviewResponseDto responseDto = createReviewResponseDto(reviewId, 1L, 1L);

        given(reviewService.getReview(reviewId)).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/reviews/{reviewId}", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reviewId))
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.rating").exists());
    }

    @Test
    @DisplayName("리뷰 단건 조회 실패 - 존재하지 않는 리뷰")
    void getReview_NotFound() throws Exception {
        // given
        Long reviewId = 999L;

        given(reviewService.getReview(reviewId))
                .willThrow(new ReviewException.ReviewNotFound(reviewId));

        // when & then
        mockMvc.perform(get("/api/reviews/{reviewId}", reviewId))
                .andExpect(status().is5xxServerError())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(ReviewException.ReviewNotFound.class));
    }

    @Test
    @DisplayName("사용자 리뷰 작성 여부 확인 성공")
    void checkUserReview_Success() throws Exception {
        // given
        Long popupId = 1L;
        Long userId = 1L;

        given(userUtil.getCurrentUserId()).willReturn(userId);
        given(reviewService.hasUserReviewedPopup(popupId, userId)).willReturn(true);

        // when & then
        mockMvc.perform(get("/api/reviews/popup/{popupId}/check", popupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReviewed").value(true))
                .andExpect(jsonPath("$.popupId").value(popupId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    @DisplayName("사용자의 특정 팝업 리뷰 조회 성공")
    void getMyReviewForPopup_Success() throws Exception {
        // given
        Long popupId = 1L;
        Long userId = 1L;
        ReviewResponseDto responseDto = createReviewResponseDto(1L, userId, popupId);

        given(userUtil.getCurrentUserId()).willReturn(userId);
        given(reviewService.getUserReviewForPopup(popupId, userId)).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/reviews/popup/{popupId}/me", popupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popupId").value(popupId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    @DisplayName("팝업 리뷰 요약 정보 조회 성공")
    void getPopupReviewSummary_Success() throws Exception {
        // given
        Long popupId = 1L;
        ReviewStatsDto statsDto = ReviewStatsDto.builder()
                .averageRating(4.2)
                .totalReviews(5L)
                .build();
        List<ReviewListResponseDto> recentReviews = Arrays.asList(
                createReviewListResponseDto(1L, 1L),
                createReviewListResponseDto(2L, 2L)
        );

        given(reviewService.getReviewStats(popupId)).willReturn(statsDto);
        given(reviewService.getRecentReviewsByPopup(popupId, 2)).willReturn(recentReviews);

        // when & then
        mockMvc.perform(get("/api/reviews/popup/{popupId}/summary", popupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.averageRating").value(4.2))
                .andExpect(jsonPath("$.stats.totalReviews").value(5))
                .andExpect(jsonPath("$.recentReviews", hasSize(2)))
                .andExpect(jsonPath("$.hasMore").value(true)); // 2개 조회했고 총 5개이므로 더 있음
    }

    @Test
    @DisplayName("잘못된 리뷰 ID 형식으로 요청시 400 오류")
    void getReview_InvalidIdFormat() throws Exception {
        // when & then
        mockMvc.perform(get("/api/reviews/{reviewId}", "invalid-id"))
                .andExpect(status().isBadRequest());
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

    private ReviewResponseDto createReviewResponseDto(Long reviewId, Long userId, Long popupId) {
        return ReviewResponseDto.builder()
                .id(reviewId)
                .popupId(popupId)
                .popupTitle("Test Popup")
                .userId(userId)
                .userName("Test User")
                .userNickname("testnick")
                .content("테스트 리뷰 내용")
                .rating(4)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isBlocked(false)
                .build();
    }

    private ReviewListResponseDto createReviewListResponseDto(Long reviewId, Long userId) {
        return ReviewListResponseDto.builder()
                .id(reviewId)
                .userId(userId)
                .userName("Test User " + userId)
                .userNickname("nick" + userId)
                .content("테스트 리뷰 내용 " + reviewId)
                .rating(4)
                .createdAt(LocalDateTime.now())
                .build();
    }
}