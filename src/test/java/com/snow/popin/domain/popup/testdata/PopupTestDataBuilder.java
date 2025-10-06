package com.snow.popin.domain.popup.testdata;

import com.snow.popin.domain.map.entity.Venue;
import com.snow.popin.domain.popup.dto.request.PopupListRequestDto;
import com.snow.popin.domain.popup.dto.response.PopupDetailResponseDto;
import com.snow.popin.domain.popup.dto.response.PopupSummaryResponseDto;
import com.snow.popin.domain.popup.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PopupTestDataBuilder {

    // ===== 기본 팝업 생성 메서드들 =====

    /**
     * 기본 팝업 생성
     */
    public static Popup createPopup(String title, PopupStatus status, Venue venue) {
        Popup popup = Popup.createForTest(title, status, venue);
        popup.setViewCountForTest(0L); // 기본 조회수 0
        return popup;
    }

    /**
     * 날짜 지정 팝업 생성
     */
    public static Popup createPopupWithDates(String title, LocalDate startDate, LocalDate endDate, Venue venue) {
        Popup popup = Popup.createForTestWithDates(title, startDate, endDate, venue);
        popup.setViewCountForTest(0L); // 기본 조회수 0
        return popup;
    }

    /**
     * 추천 팝업 생성 (isFeatured = true)
     */
    public static Popup createFeaturedPopup(String title, PopupStatus status, Venue venue) {
        Popup popup = Popup.createFeaturedForTest(title, status, venue);
        popup.setViewCountForTest(0L); // 기본 조회수 0
        return popup;
    }

    // ===== 조회수 관련 새로운 메서드들 =====

    /**
     * 조회수 지정 팝업 생성
     */
    public static Popup createPopupWithViewCount(String title, PopupStatus status, Venue venue, Long viewCount) {
        Popup popup = Popup.createForTest(title, status, venue);
        popup.setViewCountForTest(viewCount);
        return popup;
    }

    /**
     * 날짜 + 조회수 지정 팝업 생성
     */
    public static Popup createPopupWithDatesAndViewCount(String title, LocalDate startDate, LocalDate endDate,
                                                         Venue venue, Long viewCount) {
        Popup popup = Popup.createForTestWithDates(title, startDate, endDate, venue);
        popup.setViewCountForTest(viewCount);
        return popup;
    }

    /**
     * 인기 팝업 생성 (고조회수 + isFeatured = true)
     */
    public static Popup createPopularPopup(String title, PopupStatus status, Venue venue, Long viewCount) {
        Popup popup = Popup.createFeaturedForTest(title, status, venue);
        popup.setViewCountForTest(viewCount);
        return popup;
    }

    /**
     * 고조회수 팝업 생성 (1000+ 조회수)
     */
    public static Popup createHighViewCountPopup(String title, PopupStatus status, Venue venue) {
        return createPopupWithViewCount(title, status, venue, 1000L + (long)(Math.random() * 9000));
    }

    /**
     * 저조회수 팝업 생성 (100 이하 조회수)
     */
    public static Popup createLowViewCountPopup(String title, PopupStatus status, Venue venue) {
        return createPopupWithViewCount(title, status, venue, (long)(Math.random() * 100));
    }

    // ===== 장소 생성 메서드들 =====

    /**
     * 기본 장소 생성
     */
    public static Venue createVenue(String region) {
        return Venue.createForTest(region);
    }

    /**
     * 주차 가능 장소 생성
     */
    public static Venue createVenueWithParking(String region, boolean parkingAvailable) {
        return Venue.createForTestWithParking(region, parkingAvailable);
    }

    /**
     * 좌표가 있는 장소 생성 (지도용)
     */
    public static Venue createVenueWithLocation(String region, double latitude, double longitude) {
        return Venue.createForTestWithLocation(region, latitude, longitude);
    }

    // ===== 특정 시나리오별 팝업 생성 =====

    /**
     * 마감임박 팝업 생성 (3일 이내 종료)
     */
    public static Popup createDeadlineSoonPopup(String title, Venue venue) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays((long)(Math.random() * 3) + 1); // 1-3일 후 종료

        Popup popup = Popup.createForTestWithDates(title, today.minusDays(5), endDate, venue);
        popup.setStatusForTest(PopupStatus.ONGOING);
        popup.setViewCountForTest(200L);
        return popup;
    }

    /**
     * 진행중인 팝업 생성 (현재 날짜 기준)
     */
    public static Popup createOngoingPopup(String title, Venue venue) {
        LocalDate today = LocalDate.now();
        Popup popup = Popup.createForTestWithDates(title, today.minusDays(3), today.plusDays(7), venue);
        popup.setStatusForTest(PopupStatus.ONGOING);
        popup.setViewCountForTest(150L);
        return popup;
    }

    /**
     * 오픈 예정 팝업 생성
     */
    public static Popup createPlannedPopup(String title, Venue venue) {
        LocalDate today = LocalDate.now();
        Popup popup = Popup.createForTestWithDates(title, today.plusDays(2), today.plusDays(14), venue);
        popup.setStatusForTest(PopupStatus.PLANNED);
        popup.setViewCountForTest(50L);
        return popup;
    }

    /**
     * 종료된 팝업 생성
     */
    public static Popup createEndedPopup(String title, Venue venue) {
        LocalDate today = LocalDate.now();
        Popup popup = Popup.createForTestWithDates(title, today.minusDays(14), today.minusDays(1), venue);
        popup.setStatusForTest(PopupStatus.ENDED);
        popup.setViewCountForTest(500L);
        return popup;
    }

    // ===== 지역별 팝업 생성 =====

    /**
     * 강남구 팝업 생성
     */
    public static Popup createGangnamPopup(String title, PopupStatus status) {
        Venue venue = createVenue("강남구");
        return createPopup(title, status, venue);
    }

    /**
     * 홍대 팝업 생성
     */
    public static Popup createHongdaePopup(String title, PopupStatus status) {
        Venue venue = createVenue("홍대");
        return createPopup(title, status, venue);
    }

    /**
     * 종로구 팝업 생성
     */
    public static Popup createJongnoPopup(String title, PopupStatus status) {
        Venue venue = createVenue("종로구");
        return createPopup(title, status, venue);
    }

    // ===== 대량 테스트 데이터 생성 =====

    /**
     * 다양한 조회수의 팝업 목록 생성
     */
    public static List<Popup> createPopupsWithVariousViewCounts(Venue venue, int count) {
        List<Popup> popups = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long viewCount = (long)(Math.random() * 5000); // 0-5000 랜덤 조회수
            Popup popup = createPopupWithViewCount("팝업" + (i+1), PopupStatus.ONGOING, venue, viewCount);
            popups.add(popup);
        }
        return popups;
    }

    /**
     * 날짜 범위별 팝업 목록 생성
     */
    public static List<Popup> createPopupsInDateRange(Venue venue, LocalDate startDate, LocalDate endDate, int count) {
        List<Popup> popups = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(startDate, endDate);

        for (int i = 0; i < count; i++) {
            LocalDate randomStart = startDate.plusDays((long)(Math.random() * days));
            LocalDate randomEnd = randomStart.plusDays((long)(Math.random() * 14) + 1); // 1-14일 기간

            Popup popup = createPopupWithDates("기간팝업" + (i+1), randomStart, randomEnd, venue);
            popup.setStatusForTest(PopupStatus.ONGOING);
            popups.add(popup);
        }
        return popups;
    }

    // ===== Request DTO 생성 메서드들 =====

    /**
     * 기본 요청 DTO 생성
     */
    public static PopupListRequestDto createListRequest() {
        PopupListRequestDto request = new PopupListRequestDto();
        request.setPage(0);
        request.setSize(20);
        return request;
    }

    /**
     * 상태 필터 요청 DTO 생성
     */
    public static PopupListRequestDto createListRequestWithStatus(PopupStatus status) {
        PopupListRequestDto request = createListRequest();
        request.setStatus(status);
        return request;
    }

    /**
     * 지역 필터 요청 DTO 생성
     */
    public static PopupListRequestDto createListRequestWithRegion(String region) {
        PopupListRequestDto request = createListRequest();
        request.setRegion(region);
        return request;
    }

    /**
     * 정렬 요청 DTO 생성
     */
    public static PopupListRequestDto createListRequestWithSort(String sortBy) {
        PopupListRequestDto request = createListRequest();
        request.setSortBy(sortBy);
        return request;
    }

    /**
     * 날짜 필터 요청 DTO 생성
     */
    public static PopupListRequestDto createListRequestWithDateRange(LocalDate startDate, LocalDate endDate) {
        PopupListRequestDto request = createListRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        return request;
    }

    /**
     * 복합 필터 요청 DTO 생성
     */
    public static PopupListRequestDto createComplexListRequest(PopupStatus status, String region,
                                                               String sortBy, LocalDate startDate, LocalDate endDate) {
        PopupListRequestDto request = createListRequest();
        request.setStatus(status);
        request.setRegion(region);
        request.setSortBy(sortBy);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        return request;
    }

    // ===== Mock 데이터 생성 (Service 테스트용) =====

    /**
     * 조회수 포함 Mock PopupSummaryResponseDto 생성
     */
    public static PopupSummaryResponseDto createMockSummaryDto(Long id, String title, Long viewCount) {
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
                .viewCount(viewCount)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .images(Collections.emptyList())
                .venueName("테스트 장소")
                .venueAddress("테스트 주소")
                .region("강남구")
                .parkingAvailable(false)
                .build();
    }

    /**
     * 조회수 포함 Mock PopupDetailResponseDto 생성
     */
    public static PopupDetailResponseDto createMockDetailDto(Long id, String title, Long viewCount) {
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
                .isFeatured(false)
                .reservationAvailable(true)
                .waitlistAvailable(false)
                .entryFee(5000)
                .isFreeEntry(false)
                .feeDisplayText("5,000원")
                .viewCount(viewCount)
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

    // ===== 유틸리티 메서드들 =====

    /**
     * 랜덤 조회수 생성
     */
    public static Long generateRandomViewCount() {
        return (long)(Math.random() * 10000);
    }

    /**
     * 랜덤 날짜 범위 생성
     */
    public static LocalDate[] generateRandomDateRange() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays((long)(Math.random() * 30)); // 30일 전 ~ 오늘
        LocalDate end = start.plusDays((long)(Math.random() * 60) + 1); // 1-60일 기간
        return new LocalDate[]{start, end};
    }

    /**
     * 테스트용 지역 목록
     */
    public static List<String> getTestRegions() {
        return Arrays.asList("강남구", "홍대", "종로구", "신촌", "이태원", "명동", "잠실");
    }

    /**
     * 테스트용 카테고리 목록
     */
    public static List<String> getTestCategories() {
        return Arrays.asList("패션", "뷰티", "푸드", "라이프스타일", "문화", "스포츠", "기술");
    }
}