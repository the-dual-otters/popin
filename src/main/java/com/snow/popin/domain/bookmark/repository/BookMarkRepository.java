package com.snow.popin.domain.bookmark.repository;

import com.snow.popin.domain.bookmark.entity.BookMark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookMarkRepository extends JpaRepository<BookMark, Long> {

    // 특정 사용자의 특정 팝업 북마크 여부 확인
    boolean existsByUserIdAndPopupId(Long userId, Long popupId);

    // 사용자별 북마크 수 조회
    long countByUserId(Long userId);

    // 팝업별 북마크 수 조회
    long countByPopupId(Long popupId);

    // 사용자의 특정 팝업 북마크 삭제
    @Modifying
    @Query("DELETE FROM BookMark b WHERE b.userId = :userId AND b.popupId = :popupId")
    void deleteByUserIdAndPopupId(@Param("userId") Long userId, @Param("popupId") Long popupId);
}