package com.snow.popin.domain.admin.service;

import com.snow.popin.domain.space.dto.AdminSpaceListResponseDto;
import com.snow.popin.domain.space.dto.SpaceListResponseDto;
import com.snow.popin.domain.space.dto.SpaceResponseDto;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.space.repository.SpaceRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminSpaceService {

    private final SpaceRepository spaceRepo;

    /**
     * 장소 통계 조회
     */
    // TODO 공개, 비공개, 진행중, 완료 로 나누기
    public Map<String, Object> getSpaceStats(){
        Map<String, Object> stats = new HashMap<>();

        // 전체 장소 수
        long totalSpaces = spaceRepo.count();
        stats.put("totalSpaces", totalSpaces);

        // 공개 장소 수
        long visibleSpaces = spaceRepo.countByIsHidden(false);
        stats.put("visibleSpaces", visibleSpaces);

        // 비공개 장소 수
        long hiddenSpaces  = totalSpaces - visibleSpaces;
        stats.put("privateSpaces", hiddenSpaces);

        return stats;
    }

    /**
     * 장소 목록 조회 (관리자용 - JpaSpecificationExecutor 사용)
     */
    public Page<AdminSpaceListResponseDto> getSpaces(String owner, String title, Boolean isHidden, Pageable pageable) {
        log.debug("장소 목록 조회 + 필터링 - owner: {}, title: {}, isPublic: {}", owner, title, isHidden);

        // Specification 직접 작성 (팝업 방식과 동일)
        Specification<Space> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Owner fetch join (N+1 문제 방지)
            root.fetch("owner", JoinType.LEFT);

            // 소유자 필터 (이름 또는 이메일로 검색)
            if (StringUtils.hasText(owner)) {
                Join<Space, User> ownerJoin = root.join("owner", JoinType.INNER);

                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(ownerJoin.get("name")),
                        "%" + owner.toLowerCase() + "%"
                );

                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(ownerJoin.get("email")),
                        "%" + owner.toLowerCase() + "%"
                );

                predicates.add(criteriaBuilder.or(namePredicate, emailPredicate));
            }

            // 제목 필터
            if (StringUtils.hasText(title)) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"
                ));
            }

            // 공개 상태 필터
            if (isHidden != null) {
                predicates.add(criteriaBuilder.equal(root.get("isHidden"), isHidden));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 정렬 조건 추가 (최신순)
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Space> spaces = spaceRepo.findAll(spec, sortedPageable);

        return spaces.map(AdminSpaceListResponseDto::from);
    }

    /**
     * 장소 상세 조회 (관리자용)
     */
    public SpaceResponseDto getSpaceDetail(Long spaceId){
        log.info("관리자 장소 대역 상세 조회 ID: {}", spaceId);

        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다. ID:" + spaceId));

        return SpaceResponseDto.from(space);
    }

    /**
     * 장소 비활성화 (관리자용)
     */
    @Transactional
    public void toggleSpaceVisibility(Long spaceId){
        log.info("관리자 장소 상태 토글 요청 - spaceId: {}", spaceId);

        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다. ID:" + spaceId));

        boolean hidden = space.getIsHidden();

        if (hidden) {
            space.show();
            log.info("장소 활성화 완료 - spaceId: {}, title: {}", spaceId, space.getTitle());
        } else {
            space.hide();
            log.info("장소 비활성화 완료 - spaceId: {}, title: {}", spaceId, space.getTitle());
        }
    }
}