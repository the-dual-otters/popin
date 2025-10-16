package com.snow.popin.domain.bookmark.repository;

import com.snow.popin.domain.bookmark.entity.BookMark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookMarkRepositoryCustom {

    /** 사용자별 북마크 목록 (팝업/장소/카테고리 fetch join, 최신순 페이징) */
    Page<BookMark> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 사용자가 북마크한 팝업 ID 목록 */
    List<Long> findPopupIdsByUserId(Long userId);
}
