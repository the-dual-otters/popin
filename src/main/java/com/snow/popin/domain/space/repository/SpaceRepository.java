package com.snow.popin.domain.space.repository;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceRepository extends
        JpaRepository<Space, Long>,
        JpaSpecificationExecutor<Space>,
        SpaceRepositoryCustom {

    // 특정 사용자가 소유한 공간 목록
    List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDesc(User owner);

    // 권한 체크용
    Optional<Space> findByIdAndOwner(Long id, User owner);

    // 통계
    long countByIsHidden(boolean isHidden);

    // 공개 + 비숨김 최신순 페이지
    default Page<Space> findByIsPublicTrueAndIsHiddenFalseOrderByCreatedAtDesc(Pageable pageable) {
        return findPublicVisibleOrderByCreatedAtDesc(pageable);
    }
}
