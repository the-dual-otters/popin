package com.snow.popin.domain.popup.repository;

import com.snow.popin.domain.popup.entity.Popup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PopupSearchRepositoryCustom {

    /** 제목/태그 통합 검색 + 페이징 */
    Page<Popup> searchByTitleAndTags(String query, Pageable pageable);

    /** 자동완성 제안어 */
    List<String> findSuggestions(String query, int limit);
}
