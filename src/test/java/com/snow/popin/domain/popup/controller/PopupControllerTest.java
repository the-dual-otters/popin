package com.snow.popin.domain.popup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snow.popin.domain.popup.dto.response.PopupDetailResponseDto;
import com.snow.popin.domain.popup.dto.response.PopupListResponseDto;
import com.snow.popin.domain.popup.dto.response.PopupSummaryResponseDto;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.service.PopupService;
import com.snow.popin.global.exception.PopupNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PopupController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class PopupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PopupService popupService;

    @MockBean(name = "jwtUtil")
    private Object jwtUtil;

    @MockBean(name = "jwtFilter")
    private Object jwtFilter;

    // 메인 페이지 필터링 API 테스트
    @Test
    @DisplayName("전체 팝업 조회 - 기본 요청")
    void getAllPopups_기본요청_200응답() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(createMockSummaryDto(1L, "전체 팝업1")))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .size(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.getAllPopups(eq(0), eq(20), isNull())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.popups").isArray())
                .andExpect(jsonPath("$.popups", hasSize(1)))
                .andExpect(jsonPath("$.popups[0].id").value(1))
                .andExpect(jsonPath("$.popups[0].title").value("전체 팝업1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("전체 팝업 조회 - 상태 필터")
    void getAllPopups_상태필터_정상응답() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(createMockSummaryDto(1L, "진행중 팝업")))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .size(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.parseStatus("ONGOING")).thenReturn(PopupStatus.ONGOING);
        when(popupService.getAllPopups(eq(0), eq(20), eq(PopupStatus.ONGOING))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups")
                        .param("status", "ONGOING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(1)))
                .andExpect(jsonPath("$.popups[0].title").value("진행중 팝업"));
    }

    @Test
    @DisplayName("인기 팝업 조회 - 조회수 기준")
    void getPopularPopups_조회수기준_정상응답() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(
                        createMockSummaryDtoWithViewCount(1L, "인기 팝업1", 1000L),
                        createMockSummaryDtoWithViewCount(2L, "인기 팝업2", 500L)
                ))
                .totalPages(1)
                .totalElements(2L)
                .currentPage(0)
                .size(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.parseStatus("ONGOING")).thenReturn(PopupStatus.ONGOING);
        when(popupService.getPopularPopups(eq(0), eq(20))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/popular")
                        .param("status", "ONGOING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(2)))
                .andExpect(jsonPath("$.popups[0].viewCount").value(1000))
                .andExpect(jsonPath("$.popups[1].viewCount").value(500));
    }

    @Test
    @DisplayName("마감임박 팝업 조회")
    void getDeadlineSoonPopups_정상응답() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(createMockSummaryDto(1L, "마감임박 팝업")))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .size(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.parseStatus("ONGOING")).thenReturn(PopupStatus.ONGOING);
        when(popupService.getDeadlineSoonPopups(eq(0), eq(20), eq(PopupStatus.ONGOING))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/deadline")
                        .param("status", "ONGOING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(1)))
                .andExpect(jsonPath("$.popups[0].title").value("마감임박 팝업"));
    }

    @Test
    @DisplayName("지역별 + 날짜별 팝업 조회 - 7일 필터")
    void getPopupsByRegionAndDate_7일필터_정상응답() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(createMockSummaryDto(1L, "강남 7일 팝업")))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .size(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.parseStatus("ONGOING")).thenReturn(PopupStatus.ONGOING);
        when(popupService.getPopupsByRegionAndDate(
                eq("강남구"), eq("7days"), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/region-date")
                        .param("region", "강남구")
                        .param("status", "ONGOING")
                        .param("dateFilter", "7days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(1)))
                .andExpect(jsonPath("$.popups[0].title").value("강남 7일 팝업"));
    }

    @Test
    @DisplayName("지역별 + 날짜별 팝업 조회 - 사용자 지정 기간")
    void getPopupsByRegionAndDate_사용자지정기간_정상응답() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(createMockSummaryDto(1L, "사용자 지정 기간 팝업")))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .size(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);

        when(popupService.parseStatus(isNull())).thenReturn(null);
        when(popupService.getPopupsByRegionAndDate(
                eq("종로구"), eq("custom"), eq(startDate), eq(endDate), eq(0), eq(20)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/region-date")
                        .param("region", "종로구")
                        .param("dateFilter", "custom")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(1)));
    }

    @Test
    @DisplayName("AI 추천 팝업 조회 - 로그인된 사용자")
    void getAIRecommendedPopups_로그인사용자_정상응답() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(
                        createMockSummaryDtoWithBrand(1L, "AI 추천 나이키 팝업", "나이키"),
                        createMockSummaryDtoWithBrand(2L, "AI 추천 아디다스 팝업", "아디다스")
                ))
                .totalPages(1)
                .totalElements(2L)
                .currentPage(0)
                .size(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.getAIRecommendedPopups(eq(0), eq(10))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/ai-recommended")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(2)))
                .andExpect(jsonPath("$.popups[0].title").value("AI 추천 나이키 팝업"))
                .andExpect(jsonPath("$.popups[0].brandName").value("나이키"))
                .andExpect(jsonPath("$.popups[1].title").value("AI 추천 아디다스 팝업"))
                .andExpect(jsonPath("$.popups[1].brandName").value("아디다스"));
    }

    @Test
    @DisplayName("AI 추천 팝업 조회 - 비로그인 사용자 (인기 팝업 반환)")
    void getAIRecommendedPopups_비로그인사용자_인기팝업반환() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(
                        createMockSummaryDtoWithViewCount(1L, "인기 팝업1", 1500L),
                        createMockSummaryDtoWithViewCount(2L, "인기 팝업2", 1000L)
                ))
                .totalPages(1)
                .totalElements(2L)
                .currentPage(0)
                .size(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.getAIRecommendedPopups(eq(0), eq(10))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/ai-recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(2)))
                .andExpect(jsonPath("$.popups[0].viewCount").value(1500))
                .andExpect(jsonPath("$.popups[1].viewCount").value(1000));
    }

    @Test
    @DisplayName("AI 추천 팝업 조회 - 페이징 파라미터")
    void getAIRecommendedPopups_페이징파라미터_테스트() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(createMockSummaryDto(1L, "AI 추천 팝업")))
                .totalPages(3)
                .totalElements(25L)
                .currentPage(1)
                .size(10)
                .hasNext(true)
                .hasPrevious(true)
                .build();

        when(popupService.getAIRecommendedPopups(eq(1), eq(10))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/ai-recommended")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));
    }

    @Test
    @DisplayName("AI 추천 팝업 조회 - 기본 파라미터값 확인")
    void getAIRecommendedPopups_기본파라미터_테스트() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(createMockSummaryDto(1L, "기본 AI 추천")))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .size(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.getAIRecommendedPopups(eq(0), eq(10))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/ai-recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(popupService).getAIRecommendedPopups(0, 10);
    }

    @Test
    @DisplayName("AI 추천 팝업 조회 - 빈 결과")
    void getAIRecommendedPopups_빈결과_테스트() throws Exception {
        // given
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Collections.emptyList())
                .totalPages(0)
                .totalElements(0L)
                .currentPage(0)
                .size(10)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.getAIRecommendedPopups(eq(0), eq(10))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/ai-recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups").isArray())
                .andExpect(jsonPath("$.popups", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // 팝업 상세 조회 API 테스트
    @Test
    @DisplayName("팝업 상세 조회 - 정상 케이스")
    void getPopupDetail_유효한ID_팝업상세반환() throws Exception {
        // given
        Long popupId = 1L;
        PopupDetailResponseDto response = createMockDetailDto(popupId, "상세 팝업");

        when(popupService.getPopupDetail(popupId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/{popupId}", popupId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(popupId))
                .andExpect(jsonPath("$.title").value("상세 팝업"))
                .andExpect(jsonPath("$.viewCount").value(250))
                .andExpect(jsonPath("$.status").value("ONGOING"))
                .andExpect(jsonPath("$.statusDisplayText").value("진행 중"));
    }

    @Test
    @DisplayName("팝업 상세 조회 (관리자) - 조회수 증가 없음")
    void getPopupDetailForAdmin_정상응답() throws Exception {
        // given
        Long popupId = 1L;
        PopupDetailResponseDto response = createMockDetailDto(popupId, "관리자 상세 팝업");

        when(popupService.getPopupDetailForAdmin(popupId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/{popupId}/admin", popupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(popupId))
                .andExpect(jsonPath("$.title").value("관리자 상세 팝업"));
    }

    @Test
    @DisplayName("팝업 상세 조회 - 존재하지 않는 ID")
    void getPopupDetail_존재하지않는ID_404응답() throws Exception {
        // given
        Long invalidId = 999L;
        when(popupService.getPopupDetail(invalidId))
                .thenThrow(new PopupNotFoundException(invalidId));

        // when & then
        mockMvc.perform(get("/api/popups/{popupId}", invalidId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유사한 팝업 조회 - 정상 케이스")
    void getSimilarPopups_정상응답() throws Exception {
        // given
        Long popupId = 1L;
        PopupDetailResponseDto currentPopup = createMockDetailDto(popupId, "현재 팝업");
        currentPopup = PopupDetailResponseDto.builder()
                .id(popupId)
                .title("현재 팝업")
                .categoryName("패션")
                .build();

        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(
                        createMockSummaryDto(2L, "유사 팝업1"),
                        createMockSummaryDto(3L, "유사 팝업2")
                ))
                .totalPages(1)
                .totalElements(2L)
                .currentPage(0)
                .size(4)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.getPopupDetailForAdmin(popupId)).thenReturn(currentPopup);
        when(popupService.getSimilarPopups(eq("패션"), eq(popupId), eq(0), eq(4))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/{popupId}/similar", popupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(2)));
    }

    // 카테고리 및 지역별 조회 API 테스트
    @Test
    @DisplayName("카테고리별 팝업 조회")
    void getPopupsByCategory_정상응답() throws Exception {
        // given
        String categoryName = "뷰티";
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(createMockSummaryDto(1L, "뷰티 팝업")))
                .totalPages(1)
                .totalElements(1L)
                .currentPage(0)
                .size(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.getPopupsByCategory(eq(categoryName), eq(0), eq(20))).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/category/{categoryName}", categoryName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(1)))
                .andExpect(jsonPath("$.popups[0].title").value("뷰티 팝업"));
    }

    @Test
    @DisplayName("지역별 팝업 조회")
    void getPopupsByRegion_정상응답() throws Exception {
        // given
        String region = "홍대";
        List<PopupSummaryResponseDto> popups = Arrays.asList(
                createMockSummaryDto(1L, "홍대 팝업1"),
                createMockSummaryDto(2L, "홍대 팝업2")
        );

        when(popupService.getPopupsByRegion(eq(region))).thenReturn(popups);

        // when & then
        mockMvc.perform(get("/api/popups/region/{region}", region))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // 추천 팝업 API 테스트
    @Test
    @DisplayName("카테고리별 추천 팝업 조회")
    void getRecommendedPopupsByCategories_정상응답() throws Exception {
        // given
        List<Long> categoryIds = Arrays.asList(1L, 2L);
        PopupListResponseDto response = PopupListResponseDto.builder()
                .popups(Arrays.asList(
                        createMockSummaryDto(1L, "추천 팝업1"),
                        createMockSummaryDto(2L, "추천 팝업2")
                ))
                .totalPages(1)
                .totalElements(2L)
                .currentPage(0)
                .size(20)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(popupService.getRecommendedPopupsBySelectedCategories(eq(categoryIds), eq(0), eq(20)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/popups/recommended/by-categories")
                        .param("categoryIds", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popups", hasSize(2)));
    }

    @Test
    @DisplayName("잘못된 ID 형식")
    void getPopupDetail_잘못된ID형식_400응답() throws Exception {
        // when & then
        mockMvc.perform(get("/api/popups/{popupId}", "invalid-id"))
                .andExpect(status().isBadRequest());
    }

    // ===== Helper Methods =====
    private PopupSummaryResponseDto createMockSummaryDto(Long id, String title) {
        return PopupSummaryResponseDto.builder()
                .id(id)
                .title(title)
                .summary("테스트 요약")
                .period("2024.01.01 - 2024.01.08")
                .status(PopupStatus.ONGOING)
                .mainImageUrl("test-image.jpg")
                .isFeatured(false)
                .reservationAvailable(false)
                .waitlistAvailable(false)
                .entryFee(0)
                .isFreeEntry(true)
                .feeDisplayText("무료")
                .viewCount(100L) // 조회수 추가
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .images(Collections.emptyList())
                .venueName("테스트 장소")
                .venueAddress("테스트 주소")
                .region("강남구")
                .parkingAvailable(false)
                .brandId(101L)
                .brandName("테스트브랜드")
                .build();
    }

    private PopupSummaryResponseDto createMockSummaryDtoWithBrand(Long id, String title, String brandName) {
        return PopupSummaryResponseDto.builder()
                .id(id)
                .title(title)
                .summary("테스트 요약")
                .period("2024.01.01 - 2024.01.08")
                .status(PopupStatus.ONGOING)
                .mainImageUrl("test-image.jpg")
                .isFeatured(false)
                .reservationAvailable(false)
                .waitlistAvailable(false)
                .entryFee(0)
                .isFreeEntry(true)
                .feeDisplayText("무료")
                .viewCount(100L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .images(Collections.emptyList())
                .venueName("테스트 장소")
                .venueAddress("테스트 주소")
                .region("강남구")
                .parkingAvailable(false)
                .brandId(101L)
                .brandName(brandName) // 브랜드명 추가
                .build();
    }

    private PopupSummaryResponseDto createMockSummaryDtoWithViewCount(Long id, String title, Long viewCount) {
        return PopupSummaryResponseDto.builder()
                .id(id)
                .title(title)
                .summary("테스트 요약")
                .period("2024.01.01 - 2024.01.08")
                .status(PopupStatus.ONGOING)
                .mainImageUrl("test-image.jpg")
                .isFeatured(false)
                .reservationAvailable(false)
                .waitlistAvailable(false)
                .entryFee(0)
                .isFreeEntry(true)
                .feeDisplayText("무료")
                .viewCount(viewCount) // 조회수 지정
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .images(Collections.emptyList())
                .venueName("테스트 장소")
                .venueAddress("테스트 주소")
                .region("강남구")
                .parkingAvailable(false)
                .brandId(101L)
                .brandName("인기브랜드")
                .build();
    }

    private PopupDetailResponseDto createMockDetailDto(Long id, String title) {
        return PopupDetailResponseDto.builder()
                .id(id)
                .title(title)
                .summary("테스트 상세 요약")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .periodText("2024.01.01 - 2024.01.08")
                .status(PopupStatus.ONGOING)
                .statusDisplayText("진행 중")
                .mainImageUrl("test-detail-image.jpg")
                .isFeatured(true)
                .reservationAvailable(true)
                .waitlistAvailable(false)
                .entryFee(5000)
                .isFreeEntry(false)
                .feeDisplayText("5,000원")
                .viewCount(250L) // 조회수 추가
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .venueName("테스트 상세 장소")
                .venueAddress("테스트 상세 주소")
                .region("강남구")
                .parkingAvailable(true)
                .images(Collections.emptyList())
                .hours(Collections.emptyList())
                .build();
    }
}