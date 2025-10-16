package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PopupRepositoryCustom {

    List<Popup> findByStatus(PopupStatus status);

    Optional<Popup> findByIdWithDetails(Long id);

    Page<Popup> findByBrandId(Long brandId, Pageable pageable);

    Optional<Popup> findByIdWithTagsAndCategory(Long id);

    // --- 추가: PopupQueryDslRepository의 확장 기능 ---
    Page<Popup> findAllWithStatusFilter(PopupStatus status, Pageable pageable);

    Page<Popup> findPopularActivePopups(Pageable pageable);

    Page<Popup> findDeadlineSoonPopups(PopupStatus status, Pageable pageable);

    Page<Popup> findByRegionAndDateRange(String region, LocalDate startDate, LocalDate endDate, Pageable pageable);

    Page<Popup> findSimilarPopups(String categoryName, Long excludeId, Pageable pageable);

    Page<Popup> findRecommendedPopupsByCategories(List<Long> categoryIds, Pageable pageable);

    Page<Popup> findByCategoryName(String categoryName, Pageable pageable);

    List<Popup> findByIdIn(List<Long> ids);

    List<Popup> findByRegion(String region);

    List<Popup> findPopupsForMap(String region, List<Long> categoryIds);

    List<Popup> findPopupsInBounds(double southWestLat, double southWestLng, double northEastLat, double northEastLng);

    List<Popup> findPopupsWithinRadius(double lat, double lng, double radiusKm);

    List<Object[]> findMapPopupStatsByCategory(String region);

    List<Object[]> findMapPopupStatsByRegion();

    List<Popup> findPopupsToUpdateToOngoing(LocalDate today);

    List<Popup> findPopupsToUpdateToEnded(LocalDate today);
}
