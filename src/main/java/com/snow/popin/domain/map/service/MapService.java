package com.snow.popin.domain.map.service;

import com.snow.popin.domain.map.dto.PopupMapResponseDto;
import com.snow.popin.domain.map.repository.MapRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MapService {

    private final MapRepository mapRepository;
    private final PopupRepository popupRepository;

    // 현재 활성화된 팝업이 있는 지역 목록 조회
    public List<String> getAllRegions() {
        log.info("지역 목록 조회 시작");

        try {
            List<String> regions = mapRepository.findDistinctRegionsWithActivePopups();
            log.info("지역 목록 조회 완료 - 총 {}개 지역", regions.size());
            return regions;
        } catch (Exception e) {
            log.error("지역 목록 조회 실패", e);
            return List.of("서울", "인천", "부산", "대구", "대전", "광주", "울산");
        }
    }

    // 좌표가 있는 모든 지역 목록 조회
    public List<String> getRegionsWithCoordinates() {
        log.info("좌표 기반 지역 목록 조회 시작");

        List<String> regions = mapRepository.findDistinctRegionsWithCoordinates();
        log.info("좌표 기반 지역 목록 조회 완료 - 총 {}개 지역", regions.size());
        return regions;
    }

    // 지도에 표시할 팝업 목록 조회 (필터링 지원)
    public List<PopupMapResponseDto> getPopupsForMap(String region, List<Long> categoryIds) {
        log.info("지도용 팝업 목록 조회 - 지역: {}, 카테고리: {}", region, categoryIds);

        if (categoryIds != null && categoryIds.isEmpty()) {
            categoryIds = null;
        }

        List<Popup> popups = popupRepository.findPopupsForMap(region, categoryIds);

        List<PopupMapResponseDto> mapPopups = popups.stream()
                .map(PopupMapResponseDto::from)
                .filter(PopupMapResponseDto::hasValidCoordinates)
                .collect(Collectors.toList());

        log.info("지도용 팝업 조회 완료 - 총 {}개", mapPopups.size());
        return mapPopups;
    }

    // 특정 지역 범위 내 팝업 조회 (바운딩 박스 기반)
    public List<PopupMapResponseDto> getPopupsInBounds(double southWestLat, double southWestLng,
                                                       double northEastLat, double northEastLng) {
        log.info("범위 내 팝업 조회 - SW({}, {}), NE({}, {})",
                southWestLat, southWestLng, northEastLat, northEastLng);

        // 좌표 유효성 검증
        if (!isValidCoordinateRange(southWestLat, southWestLng, northEastLat, northEastLng)) {
            log.warn("유효하지 않은 좌표 범위");
            return List.of();
        }

        List<Popup> popups = popupRepository.findPopupsInBounds(
                southWestLat, southWestLng, northEastLat, northEastLng);

        List<PopupMapResponseDto> mapPopups = popups.stream()
                .map(PopupMapResponseDto::from)
                .filter(PopupMapResponseDto::hasValidCoordinates)
                .collect(Collectors.toList());

        log.info("범위 내 팝업 조회 완료 - 총 {}개", mapPopups.size());
        return mapPopups;
    }

    // 내 주변 팝업 조회 (반경 기반)
    public List<PopupMapResponseDto> getNearbyPopups(double latitude, double longitude, double radiusKm) {
        log.info("주변 팝업 조회 - 위치: ({}, {}), 반경: {}km", latitude, longitude, radiusKm);

        // 좌표 및 반경 유효성 검증
        if (!isValidCoordinate(latitude, longitude) || radiusKm <= 0 || radiusKm > 100) {
            log.warn("유효하지 않은 좌표 또는 반경: ({}, {}), {}km", latitude, longitude, radiusKm);
            return List.of();
        }

        List<Popup> popups = popupRepository.findPopupsWithinRadius(latitude, longitude, radiusKm);

        List<PopupMapResponseDto> mapPopups = popups.stream()
                .map(PopupMapResponseDto::from)
                .filter(PopupMapResponseDto::hasValidCoordinates)
                .collect(Collectors.toList());

        log.info("주변 팝업 조회 완료 - 총 {}개", mapPopups.size());
        return mapPopups;
    }

    /**
     * 카테고리별 지도 팝업 통계 조회
     */
    public Map<String, Long> getMapPopupStatsByCategory(String region) {
        log.info("카테고리별 지도 팝업 통계 조회 - 지역: {}", region);

        List<Object[]> results = popupRepository.findMapPopupStatsByCategory(region);

        Map<String, Long> stats = results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        log.info("카테고리별 통계 조회 완료 - {}개 카테고리", stats.size());
        return stats;
    }

    /**
     * 지역별 지도 팝업 통계 조회
     */
    public Map<String, Long> getMapPopupStatsByRegion() {
        log.info("지역별 지도 팝업 통계 조회 시작");

        List<Object[]> results = popupRepository.findMapPopupStatsByRegion();

        Map<String, Long> stats = results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],  // 지역명
                        row -> ((Number) row[1]).longValue(),// 개수
                        Long::sum
                ));

        log.info("지역별 통계 조회 완료 - {}개 지역", stats.size());
        return stats;
    }

    // 지도 팝업 검색 (제목, 태그, 지역으로 검색)
    public List<PopupMapResponseDto> searchMapPopups(String query, String region, List<Long> categoryIds) {
        log.info("지도 팝업 검색 - 검색어: {}, 지역: {}, 카테고리: {}", query, region, categoryIds);

        // 기본적으로는 필터링된 팝업을 가져오고, 추후 검색 기능 확장 가능
        List<PopupMapResponseDto> popups = getPopupsForMap(region, categoryIds);

        if (query != null && !query.trim().isEmpty()) {
            final String searchTerm = query.toLowerCase(java.util.Locale.ROOT).trim();
            popups = popups.stream()
                    .filter(p -> {
                        final String t = p.getTitle();
                        final String s = p.getSummary();
                        final String v = p.getVenueName();
                        return (t != null && t.toLowerCase(java.util.Locale.ROOT).contains(searchTerm))
                                || (s != null && s.toLowerCase(java.util.Locale.ROOT).contains(searchTerm))
                                || (v != null && v.toLowerCase(java.util.Locale.ROOT).contains(searchTerm));
                        })
                    .collect(Collectors.toList());
        }

        log.info("지도 팝업 검색 완료 - 총 {}개", popups.size());
        return popups;
    }

    //좌표 유효성 검증
    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    // 좌표 범위 유효성 검증
    private boolean isValidCoordinateRange(double swLat, double swLng, double neLat, double neLng) {
        return isValidCoordinate(swLat, swLng) &&
                isValidCoordinate(neLat, neLng) &&
                swLat <= neLat && swLng <= neLng;
    }
}
