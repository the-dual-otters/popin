package com.snow.popin.domain.bookmark.repository;

import com.snow.popin.domain.bookmark.entity.BookMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BookMarkRepository extends
        JpaRepository<BookMark, Long>,
        BookMarkRepositoryCustom {

    // 특정 사용자의 특정 팝업 북마크 여부
    boolean existsByUserIdAndPopupId(Long userId, Long popupId);

    // 사용자별/팝업별 카운트
    long countByUserId(Long userId);
    long countByPopupId(Long popupId);

    // 사용자의 특정 팝업 북마크 단건 삭제 (반환값: 삭제된 row 수)
    @Transactional
    long deleteByUserIdAndPopupId(Long userId, Long popupId);
}
