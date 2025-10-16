package com.snow.popin.domain.popup.service;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.popup.dto.response.*;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.recommendation.dto.AiRecommendationResponseDto;
import com.snow.popin.domain.recommendation.service.AiRecommendationService;
import com.snow.popin.global.exception.PopupNotFoundException;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PopupService {

    private final PopupRepository popupRepository;
    private final AiRecommendationService aiRecommendationService;
    private final BrandRepository brandRepository;
    private final UserUtil userUtil;

    // ===== 메인 페이지 필터링 API =====

    // 전체 팝업 조회
    public PopupListResponseDto getAllPopups(int page, int size, PopupStatus status) {
        log.info("전체 팝업 조회 - page: {}, size: {}, status: {}", page, size, status);

        Pageable pageable = createPageable(page, size);
        Page<Popup> popupPage = popupRepository.findAllWithStatusFilter(status, pageable);

        List<PopupSummaryResponseDto> popupDtos = convertToSummaryDtos(popupPage.getContent());

        log.info("전체 팝업 조회 완료 - 총 {}개", popupPage.getTotalElements());
        return PopupListResponseDto.of(popupPage, popupDtos);
    }

    // 인기 팝업 조회
    public PopupListResponseDto getPopularPopups(int page, int size) {
        log.info("인기 팝업 조회 시작 - page: {}, size: {}", page, size);

        // 인기 팝업은 최대 20개로 제한
        int maxPopularItems = 20;
        int remainingItems = maxPopularItems - (page * size);

        if (remainingItems <= 0) {
            return PopupListResponseDto.empty(page, size);
        }

        int adjustedSize = Math.min(size, remainingItems);
        Pageable pageable = createPageable(page, adjustedSize);

        // 진행중/예정 상태만 조회하는 새 메서드 사용
        Page<Popup> popupPage = popupRepository.findPopularActivePopups(pageable);

        List<PopupSummaryResponseDto> popupDtos = popupPage.getContent()
                .stream()
                .map(PopupSummaryResponseDto::from)
                .collect(Collectors.toList());

        log.info("인기 팝업 조회 완료 - 총 {}개 (ONGOING/PLANNED만)", popupDtos.size());

        return PopupListResponseDto.of(popupPage, popupDtos);
    }

    // 마감임박 팝업 조회
    public PopupListResponseDto getDeadlineSoonPopups(int page, int size, PopupStatus status) {
        log.info("마감임박 팝업 조회 - page: {}, size: {}, status: {}", page, size, status);

        Pageable pageable = createPageable(page, size);
        Page<Popup> popupPage = popupRepository.findDeadlineSoonPopups(status, pageable);

        List<PopupSummaryResponseDto> popupDtos = convertToSummaryDtos(popupPage.getContent());

        log.info("마감임박 팝업 조회 완료 - 총 {}개", popupPage.getTotalElements());
        return PopupListResponseDto.of(popupPage, popupDtos);
    }

    // 지역별 + 날짜별 팝업 조회
    public PopupListResponseDto getPopupsByRegionAndDate(
            String region, String dateFilter,
            LocalDate customStartDate, LocalDate customEndDate,
            int page, int size) {

        log.info("지역별 날짜별 팝업 조회 - region: {}, dateFilter: {}", region, dateFilter);

        // 날짜 범위 계산
        LocalDate[] dateRange = calculateDateRange(dateFilter, customStartDate, customEndDate);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];

        Pageable pageable = createPageable(page, size);

        // status 파라미터 제거하고 호출
        Page<Popup> popupPage = popupRepository.findByRegionAndDateRange(
                region, startDate, endDate, pageable);

        List<PopupSummaryResponseDto> popupDtos = convertToSummaryDtos(popupPage.getContent());

        log.info("지역별 날짜별 팝업 조회 완료 - 총 {}개", popupPage.getTotalElements());
        return PopupListResponseDto.of(popupPage, popupDtos);
    }

    /**
     * AI 추천 팝업 조회 (기존 메서드 활용)
     * - 로그인한 경우: 사용자별 개인화 추천
     * - 로그인하지 않은 경우: 기존 인기 팝업 반환
     */
    public PopupListResponseDto getAIRecommendedPopups(int page, int size) {
        log.info("AI 추천 팝업 조회 - page: {}, size: {}", page, size);

        try {
            // 로그인 상태 확인
            if (!userUtil.isAuthenticated()) {
                log.info("비로그인 사용자 - 기존 인기 팝업으로 대체");
                return getPopularPopups(page, size);
            }

            Long userId = userUtil.getCurrentUserId();
            log.info("로그인 사용자 {} - AI 개인화 추천 시작", userId);

            // AI 추천 서비스 호출
            AiRecommendationResponseDto aiRecommendation =
                    aiRecommendationService.getPersonalizedRecommendations(userId, size);

            if (!aiRecommendation.isSuccess() || aiRecommendation.getRecommendedPopupIds().isEmpty()) {
                log.warn("AI 추천 실패 - 기존 인기 팝업으로 대체");
                return getPopularPopups(page, size);
            }

            // 추천된 팝업 ID로 팝업 조회
            List<Popup> recommendedPopups = popupRepository.findByIdIn(
                    aiRecommendation.getRecommendedPopupIds()
            );

            // AI가 추천한 순서대로 정렬
            recommendedPopups = sortPopupsByIdOrder(recommendedPopups,
                    aiRecommendation.getRecommendedPopupIds());

            // DTO 변환 (브랜드 정보 포함)
            List<PopupSummaryResponseDto> popupDtos = convertToSummaryDtosWithBrand(recommendedPopups);

            // 페이지 처리
            Pageable pageable = PageRequest.of(page, size);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), popupDtos.size());

            List<PopupSummaryResponseDto> pagedDtos = start < popupDtos.size() ?
                    popupDtos.subList(start, end) : List.of();

            List<Popup> pagedEntities = start < recommendedPopups.size()
                    ? recommendedPopups.subList(start, Math.min(start + pageable.getPageSize(), recommendedPopups.size()))
                    : List.of();
            Page<Popup> popupPage = new PageImpl<>(pagedEntities, pageable, recommendedPopups.size());

            log.info("AI 추천 완료 - 총 {}개 추천, 이유: {}",
                    recommendedPopups.size(), aiRecommendation.getReasoning());

            return PopupListResponseDto.of(popupPage, pagedDtos);

        } catch (Exception e) {
            log.error("AI 추천 처리 중 오류 발생", e);
            return getPopularPopups(page, size);
        }
    }

    /**
     * AI 추천 순서대로 팝업 정렬
     */
    private List<Popup> sortPopupsByIdOrder(List<Popup> popups, List<Long> orderedIds) {
        Map<Long, Popup> popupMap = popups.stream()
                .collect(Collectors.toMap(Popup::getId, popup -> popup));

        return orderedIds.stream()
                .map(popupMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 브랜드 정보를 포함한 DTO 변환 (AI 추천용)
     */
    private List<PopupSummaryResponseDto> convertToSummaryDtosWithBrand(List<Popup> popups) {
        if (popups.isEmpty()) {
            return List.of();
        }

        try {
            // 브랜드 ID 추출
            Set<Long> brandIds = popups.stream()
                    .map(Popup::getBrandId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 브랜드 정보 배치 조회
            Map<Long, String> brandMap = brandRepository.findAllById(brandIds)
                    .stream()
                    .collect(Collectors.toMap(
                            Brand::getId,
                            Brand::getName,
                            (existing, replacement) -> existing
                    ));

            // DTO 변환
            return popups.stream()
                    .map(popup -> {
                        String brandName = brandMap.getOrDefault(popup.getBrandId(), "브랜드");
                        return PopupSummaryResponseDto.fromWithBrand(popup, brandName);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("브랜드 정보 포함 DTO 변환 실패, 기본 DTO로 대체", e);
            // 오류 시 기본 DTO 변환 사용
            return popups.stream()
                    .map(PopupSummaryResponseDto::from)
                    .collect(Collectors.toList());
        }
    }

    // ===== 팝업 상세 조회 =====

    // 팝업 상세 조회 (조회수 증가)
    @Transactional
    public PopupDetailResponseDto getPopupDetail(Long popupId) {
        log.info("팝업 상세 조회 - popupId: {}", popupId);

        Popup popup = popupRepository.findByIdWithDetails(popupId)
                .orElseThrow(() -> new PopupNotFoundException(popupId));

        // 실시간 상태 업데이트 확인
        boolean statusChanged = popup.updateStatus();
        if (statusChanged) {
            log.info("팝업 ID {}의 상태가 실시간으로 업데이트됨: {}", popup.getId(), popup.getStatus());
            popupRepository.save(popup);
        }

        // 조회수 증가
        popup.incrementViewCount();
        log.info("팝업 조회수 증가 - ID: {}, 현재 조회수: {}", popup.getId(), popup.getViewCount());

        return PopupDetailResponseDto.from(popup);
    }

    // 팝업 상세 조회 (조회수 증가 없음 - 관리자용)
    public PopupDetailResponseDto getPopupDetailForAdmin(Long popupId) {
        log.info("팝업 상세 조회 (관리자) - popupId: {}", popupId);

        Popup popup = popupRepository.findByIdWithDetails(popupId)
                .orElseThrow(() -> new PopupNotFoundException(popupId));

        return PopupDetailResponseDto.from(popup);
    }

    // ===== 추천 및 유사 팝업 조회 =====

    // 유사한 팝업 조회 (같은 카테고리)
    public PopupListResponseDto getSimilarPopups(String categoryName, Long excludePopupId, int page, int size) {
        log.info("유사한 팝업 조회 - 카테고리: {}, 제외 ID: {}", categoryName, excludePopupId);

        if (categoryName == null || categoryName.trim().isEmpty()) {
            log.warn("카테고리명이 없어서 빈 결과 반환");
            return PopupListResponseDto.empty(0, 20);
        }

        try {
            Pageable pageable = createPageable(page, size);
            Page<Popup> popupPage = popupRepository.findSimilarPopups(categoryName, excludePopupId, pageable);

            List<PopupSummaryResponseDto> popupDtos = convertToSummaryDtos(popupPage.getContent());

            log.info("유사한 팝업 조회 완료 - 총 {}개", popupPage.getTotalElements());
            return PopupListResponseDto.of(popupPage, popupDtos);

        } catch (Exception e) {
            log.error("유사한 팝업 조회 실패 - 카테고리: {}", categoryName, e);
            return PopupListResponseDto.empty(page, size);
        }
    }

    // 카테고리별 추천 팝업 조회
    public PopupListResponseDto getRecommendedPopupsBySelectedCategories(
            List<Long> categoryIds, int page, int size) {
        log.info("선택된 카테고리 기반 추천 팝업 조회 - categoryIds: {}", categoryIds);

        Pageable pageable = createPageable(page, size);
        if (categoryIds == null || categoryIds.isEmpty()) {
            Page<Popup> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            return PopupListResponseDto.of(emptyPage, List.of());
        }

        Page<Popup> popupPage = popupRepository.findRecommendedPopupsByCategories(categoryIds, pageable);

        List<PopupSummaryResponseDto> popupDtos = convertToSummaryDtos(popupPage.getContent());

        log.info("선택된 카테고리 추천 팝업 조회 완료 - 총 {}개", popupPage.getTotalElements());
        return PopupListResponseDto.of(popupPage, popupDtos);
    }

    // ===== 카테고리 및 지역별 조회 =====

    // 카테고리별 팝업 조회
    public PopupListResponseDto getPopupsByCategory(String categoryName, int page, int size) {
        log.info("카테고리별 팝업 조회 - 카테고리: {}", categoryName);

        try {
            Pageable pageable = createPageable(page, size);
            Page<Popup> popupPage = popupRepository.findByCategoryName(categoryName, pageable);

            List<PopupSummaryResponseDto> popupDtos = convertToSummaryDtos(popupPage.getContent());

            log.info("카테고리별 팝업 조회 완료 - 총 {}개", popupPage.getTotalElements());
            return PopupListResponseDto.of(popupPage, popupDtos);

        } catch (Exception e) {
            log.error("카테고리별 팝업 조회 실패 - 카테고리: {}", categoryName, e);
            return PopupListResponseDto.empty(page, size);
        }
    }

    // 지역별 팝업 조회
    public List<PopupSummaryResponseDto> getPopupsByRegion(String region) {
        log.info("지역별 팝업 조회 - region: {}", region);

        List<Popup> popups = popupRepository.findByRegion(region);

        return popups.stream()
                .map(PopupSummaryResponseDto::from)
                .collect(Collectors.toList());
    }

    // ===== 유틸리티 메서드들 =====

    // PopupStatus 문자열을 Enum으로 변환
    public PopupStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty() || "전체".equals(status)) {
            return null; // 전체 조회
        }

        try {
            return PopupStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 상태값: {}", status);
            return null;
        }
    }

    // 페이지네이션 객체 생성
    private Pageable createPageable(int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(Math.max(1, size), 100);
        return PageRequest.of(validPage, validSize);
    }

    // 날짜 범위 계산
    private LocalDate[] calculateDateRange(String dateFilter, LocalDate customStartDate, LocalDate customEndDate) {
        LocalDate startDate = null;
        LocalDate endDate = null;

        // 직접입력인 경우 우선 처리
        if (customStartDate != null && customEndDate != null) {
            startDate = customStartDate;
            endDate = customEndDate;
            return new LocalDate[]{startDate, endDate};
        }

        if (dateFilter != null) {
            LocalDate today = LocalDate.now();
            switch (dateFilter) {
                case "today":
                    startDate = today;
                    endDate = today;
                    break;
                case "7days":
                    startDate = today;
                    endDate = today.plusDays(7);
                    break;
                case "14days":
                    startDate = today;
                    endDate = today.plusDays(14);
                    break;
                case "custom":
                    startDate = customStartDate;
                    endDate = customEndDate;
                    break;
                default:
                    break;
            }
        }

        return new LocalDate[]{startDate, endDate};
    }

    // Popup 리스트를 PopupSummaryResponseDto 리스트로 변환
    private List<PopupSummaryResponseDto> convertToSummaryDtos(List<Popup> popups) {
        return popups.stream()
                .map(PopupSummaryResponseDto::from)
                .collect(Collectors.toList());
    }
}