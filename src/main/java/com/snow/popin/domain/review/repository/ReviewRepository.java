package com.snow.popin.domain.review.repository;

import com.snow.popin.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends
        JpaRepository<Review, Long>,
        ReviewRepositoryCustom {

    // 특정 팝업의 차단되지 않은 리뷰 (페이징 + 정렬은 Pageable로)
    Page<Review> findByPopup_IdAndIsBlockedFalse(Long popupId, Pageable pageable);

    // 사용자별 리뷰
    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 최신 10개
    List<Review> findTop10ByPopupIdAndIsBlockedFalseOrderByCreatedAtDesc(Long popupId);

    // 존재 여부 / 단건 조회 / 카운트
    boolean existsByPopupIdAndUserId(Long popupId, Long userId);
    Optional<Review> findByPopup_IdAndUserId(Long popupId, Long userId);
    long countByPopupIdAndIsBlockedFalse(Long popupId);
}
