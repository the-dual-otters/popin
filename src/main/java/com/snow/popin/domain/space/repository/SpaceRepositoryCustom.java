package com.snow.popin.domain.space.repository;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SpaceRepositoryCustom {
    List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDesc(User owner);
    Optional<Space> findByIdAndOwner(Long id, User owner);
    long countByIsHidden(boolean isHidden);
    List<Space> searchSpacesWithJoins(String keyword, String location, Integer minArea, Integer maxArea);
    List<Space> findByOwnerAndIsHiddenFalseOrderByCreatedAtDescWithJoins(User owner);
    Page<Space> findByIsPublicTrueAndIsHiddenFalseOrderByCreatedAtDesc(Pageable pageable);
}