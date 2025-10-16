package com.snow.popin.domain.space.repository;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SpaceRepositoryCustom {

    /** 공개 + 비숨김 최신순 페이지 */
    Page<Space> findPublicVisibleOrderByCreatedAtDesc(Pageable pageable);

    /** Provider가 등록한 공간 목록 (fetch join으로 N+1 방지) */
    List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDescWithJoins(User owner);

    /** 키워드/위치/면적 필터 + 조인검색 (N+1 방지) */
    List<Space> searchSpacesWithJoins(String keyword, String location, Integer minArea, Integer maxArea);
}
