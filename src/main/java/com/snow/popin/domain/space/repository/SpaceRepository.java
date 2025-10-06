package com.snow.popin.domain.space.repository;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long>, JpaSpecificationExecutor<Space> {

    // 특정 사용자가 소유한 공간 목록 조회
    List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDesc(User owner);

    // 특정 사용자 소유의 특정 공간 조회 (권한 체크용)
    Optional<Space> findByIdAndOwner(Long id, User owner);

    // 통계용 메서드들
    long countByIsHidden(boolean isHidden);

    //검색용 쿼리
    @Query("SELECT DISTINCT s FROM Space s " +
            "JOIN FETCH s.owner o " +
            "LEFT JOIN FETCH s.venue v " +
            "WHERE s.isPublic = true AND s.isHidden = false AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:location IS NULL OR :location = '' OR " +
            " LOWER(s.address) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
            " (s.venue IS NOT NULL AND (" +
            "  LOWER(v.roadAddress) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
            "  LOWER(v.jibunAddress) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
            "  LOWER(v.detailAddress) LIKE LOWER(CONCAT('%', :location, '%'))))) AND " +
            "(:minArea IS NULL OR s.areaSize >= :minArea) AND " +
            "(:maxArea IS NULL OR s.areaSize <= :maxArea) " +
            "ORDER BY s.createdAt DESC")
    List<Space> searchSpacesWithJoins(@Param("keyword") String keyword,
                                      @Param("location") String location,
                                      @Param("minArea") Integer minArea,
                                      @Param("maxArea") Integer maxArea);

    // Provider가 등록한 공간 목록 조회 (join으로 N+1 방지)
    @Query("SELECT DISTINCT s FROM Space s " +
            "JOIN FETCH s.owner o " +
            "LEFT JOIN FETCH s.venue v " +
            "WHERE s.owner = :owner AND s.isHidden = false " +
            "ORDER BY s.createdAt DESC")
    List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDescWithJoins(@Param("owner") User owner);

    Page<Space> findByIsPublicTrueAndIsHiddenFalseOrderByCreatedAtDesc(Pageable pageable);

}