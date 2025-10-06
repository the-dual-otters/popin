package com.snow.popin.domain.map.controller;

import com.snow.popin.domain.map.dto.PopupMapResponseDto;
import com.snow.popin.domain.map.service.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
@Validated
public class MapController {

    private final MapService mapService;

    // 현재 활성화된 팝업이 있는 지역 목록 조회
    @GetMapping("/regions")
    public ResponseEntity<List<String>> getAllRegions() {
        log.info("지역 목록 조회 API 호출");
        List<String> regions = mapService.getAllRegions();
        return ResponseEntity.ok(regions);
    }

    // 좌표가 있는 지역 목록 조회
    @GetMapping("/regions/coordinates")
    public ResponseEntity<List<String>> getRegionsWithCoordinates() {
        log.info("좌표 기반 지역 목록 조회 API 호출");
        List<String> regions = mapService.getRegionsWithCoordinates();
        return ResponseEntity.ok(regions);
    }

    // 지도에 표시할 팝업 목록 조회 (필터링 지원)
    @GetMapping("/popups")
    public ResponseEntity<List<PopupMapResponseDto>> getPopupsForMap(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<Long> categoryIds) {

        log.info("지도용 팝업 목록 조회 API 호출 - 지역: {}, 카테고리: {}", region, categoryIds);
        List<PopupMapResponseDto> popups = mapService.getPopupsForMap(region, categoryIds);
        return ResponseEntity.ok(popups);
    }

    // 특정 범위 내 팝업 조회 (지도 이동/줌 시 사용)
    @GetMapping("/popups/bounds")
    public ResponseEntity<List<PopupMapResponseDto>> getPopupsInBounds(
            @RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") double southWestLat,
            @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") double southWestLng,
            @RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") double northEastLat,
            @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") double northEastLng) {

        log.info("범위 내 팝업 조회 API 호출 - SW({}, {}), NE({}, {})",
                southWestLat, southWestLng, northEastLat, northEastLng);

        List<PopupMapResponseDto> popups = mapService.getPopupsInBounds(
                southWestLat, southWestLng, northEastLat, northEastLng);
        return ResponseEntity.ok(popups);
    }

    // 내 주변 팝업 조회 (현재 위치 기반)
    @GetMapping("/popups/nearby")
    public ResponseEntity<List<PopupMapResponseDto>> getNearbyPopups(
            @RequestParam @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") double lat,
            @RequestParam @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") double lng,
            @RequestParam(defaultValue = "10.0") @DecimalMin(value = "0.1") @DecimalMax(value = "100.0") double radiusKm) {

        log.info("주변 팝업 조회 API 호출 - 위치: ({}, {}), 반경: {}km", lat, lng, radiusKm);
        List<PopupMapResponseDto> popups = mapService.getNearbyPopups(lat, lng, radiusKm);
        return ResponseEntity.ok(popups);
    }

    //카테고리별 지도 팝업 통계 조회
    @GetMapping("/popups/stats/category")
    public ResponseEntity<Map<String, Long>> getMapPopupStatsByCategory(
            @RequestParam(required = false) String region) {

        log.info("카테고리별 지도 팝업 통계 조회 API 호출 - 지역: {}", region);
        Map<String, Long> stats = mapService.getMapPopupStatsByCategory(region);
        return ResponseEntity.ok(stats);
    }

    //지역별 지도 팝업 통계 조회
    @GetMapping("/popups/stats/region")
    public ResponseEntity<Map<String, Long>> getMapPopupStatsByRegion() {
        log.info("지역별 지도 팝업 통계 조회 API 호출");
        Map<String, Long> stats = mapService.getMapPopupStatsByRegion();
        return ResponseEntity.ok(stats);
    }

    // 지도 팝업 검색
    @GetMapping("/popups/search")
    public ResponseEntity<List<PopupMapResponseDto>> searchMapPopups(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<Long> categoryIds) {

        log.info("지도 팝업 검색 API 호출 - 검색어: {}, 지역: {}, 카테고리: {}", query, region, categoryIds);

        if (categoryIds != null && categoryIds.isEmpty()) {
            categoryIds = null;
        }

        List<PopupMapResponseDto> popups = mapService.searchMapPopups(query, region, categoryIds);
        return ResponseEntity.ok(popups);
    }
}