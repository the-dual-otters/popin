package com.snow.popin.domain.admin.service;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.mypage.host.repository.HostRepository;
import com.snow.popin.domain.popup.dto.response.PopupAdminResponse;
import com.snow.popin.domain.popup.dto.response.PopupAdminStatusUpdateResponse;
import com.snow.popin.domain.popup.dto.response.PopupStatsResponse;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 관리자 팝업 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPopupService {

    private final PopupRepository popupRepo;
    private final BrandRepository brandRepo;
    private final HostRepository hostRepo;

    /**
     * 팝업 통게 조회
     */
    public PopupStatsResponse getPopupStats() {
        log.debug("팝업 통계 조회 시작");

        return PopupStatsResponse.builder()
                .total(popupRepo.count())
                .planning(popupRepo.countByStatus(PopupStatus.PLANNED))
                .ongoing(popupRepo.countByStatus(PopupStatus.ONGOING))
                .completed(popupRepo.countByStatus(PopupStatus.ENDED))
                .build();
    }

    /**
     * 관리자용 팝업 목록 조회 (필터링 및 검색 지원)
     */
    public Page<PopupAdminResponse> getPopupsForAdmin(
            Pageable pageable, PopupStatus status, String category, String keyword) {
        log.debug("관리자용 팝업 목록 조회 - 상태: {}, 카테고리: {}, 키워드: {}", status, category, keyword);

        Specification<Popup> spec = createPopupSpecification(status, category, keyword);
        Page<Popup> popups = popupRepo.findAll(spec, pageable);

        // 팝업에서 사용된 브랜드 ID들을 수집하여 한 번에 조회
        List<Long> brandIds = popups.getContent().stream()
                .map(Popup::getBrandId)
                .distinct()
                .collect(Collectors.toList());

        // 브랜드 정보를 일괄 조회하여 Map으로 변환 (brandId -> Brand)
        Map<Long, Brand> brandMap = getBrandMap(brandIds);

        // 각 팝업에 대해 브랜드와 주최자 정보를 포함하여 변환
        return popups.map(popup -> {
            Brand brand = brandMap.get(popup.getBrandId());
            if (brand == null) {
                throw new GeneralException(ErrorCode.NOT_FOUND);
            }
            return PopupAdminResponse.fromWithBrand(popup, brand);
        });
    }

    /**
     * 팝업 관리 정보 조회 (브랜드 정보 포함)
     */
    public PopupAdminResponse getPopupForAdmin(Long popupId) {
        Popup popup = popupRepo.findById(popupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POPUP_NOT_FOUND));

        // 브랜드 정보 조회
        Brand brand = brandRepo.findById(popup.getBrandId())
                .orElseThrow(() -> {
                    log.warn("브랜드 정보를 찾을 수 없음: brandId={}", popup.getBrandId());
                    return new GeneralException(ErrorCode.NOT_FOUND);
                });

        return PopupAdminResponse.fromWithBrand(popup, brand);
    }

    /**
     * 브랜드 ID 목록으로부터 브랜드 Map을 생성
     * 존재하지 않는 브랜드는 null로 처리하여 안전하게 처리
     */
    private Map<Long, Brand> getBrandMap(List<Long> brandIds) {
        if (brandIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<Brand> brands = brandRepo.findAllById(brandIds);
            return brands.stream()
                    .collect(Collectors.toMap(Brand::getId, Function.identity()));
        } catch (Exception e) {
            log.warn("브랜드 정보 일괄 조회 중 오류 발생: brandIds={}", brandIds, e);
            // 빈 Map 반환하여 null 처리되도록 함
            return Map.of();
        }
    }

    /**
     * 팝업 검색 조건 생성
     */
    private Specification<Popup> createPopupSpecification(PopupStatus status, String category, String keyword) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 상태 필터
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // 카테고리 필터
            if (StringUtils.hasText(category)) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("name"), category));
            }

            // 키워드 검색 (팝업명 + 주최자명)
            if (StringUtils.hasText(keyword)) {
                String searchKeyword = "%" + keyword.toLowerCase() + "%";

                // 팝업명으로 검색
                Predicate titlePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")), searchKeyword);

                // 브랜드명으로 검색
                Subquery<Long> brandSubquery = query.subquery(Long.class);
                Root<Brand> brandRoot = brandSubquery.from(Brand.class);
                brandSubquery.select(brandRoot.get("id"))
                        .where(criteriaBuilder.like(
                                criteriaBuilder.lower(brandRoot.get("name")),
                                searchKeyword));

                Predicate brandNamePredicate = criteriaBuilder.in(root.get("brandId")).value(brandSubquery);

                predicates.add(criteriaBuilder.or(titlePredicate, brandNamePredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 팝업 상태 변경
     */
    @Transactional
    public PopupAdminStatusUpdateResponse updatePopupStatus(Long popupId, PopupStatus status) {
        log.info("팝업 상태 변경 시작 - popupId: {}, 새로운 상태: {}", popupId, status);

        Popup popup = popupRepo.findById(popupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.POPUP_NOT_FOUND));

        popup.AdminUpdateStatus(status);
        popupRepo.save(popup);

        log.info("팝업 상태 변경 완료 - popupId: {}, 변경된 상태: {}", popupId, status);

        return PopupAdminStatusUpdateResponse.of(popup);
    }
}
