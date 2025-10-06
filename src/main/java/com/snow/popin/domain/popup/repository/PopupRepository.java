package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PopupRepository extends JpaRepository<Popup, Long>, JpaSpecificationExecutor<Popup> {

    /**
     * 특정 상태의 팝업 조회 (AI 추천용)
     */
    List<Popup> findByStatus(PopupStatus status);

    // ===== 팝업 상세 조회 =====

    @EntityGraph(attributePaths = {"images", "hours", "venue", "tags", "category"})
    @Query("SELECT p FROM Popup p " +
            "LEFT JOIN FETCH p.venue v " +
            "LEFT JOIN FETCH p.category c " +
            "WHERE p.id = :id")
    Optional<Popup> findByIdWithDetails(@Param("id") Long id);

    // ===== 관리용 메서드들 =====

    @Query("SELECT p FROM Popup p WHERE p.brandId = :brandId ORDER BY p.createdAt DESC")
    Page<Popup> findByBrandId(@Param("brandId") Long brandId, Pageable pageable);

    Optional<Popup> findFirstByTitle(String title);

    long countByStatus(PopupStatus status);
    long count();

    @Query("SELECT p FROM Popup p " +
            "LEFT JOIN FETCH p.tags " +
            "LEFT JOIN FETCH p.category " +
            "WHERE p.id = :id")
    Optional<Popup> findByIdWithTagsAndCategory(@Param("id") Long id);
}