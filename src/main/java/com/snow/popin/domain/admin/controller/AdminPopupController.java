package com.snow.popin.domain.admin.controller;

import com.snow.popin.domain.admin.service.AdminPopupService;
import com.snow.popin.domain.popup.dto.request.PopupStatusUpdateRequest;
import com.snow.popin.domain.popup.dto.response.PopupAdminResponse;
import com.snow.popin.domain.popup.dto.response.PopupAdminStatusUpdateResponse;
import com.snow.popin.domain.popup.dto.response.PopupStatsResponse;
import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 관리자 팝업 관리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/popups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPopupController {

    private final AdminPopupService adminPopupService;

    /**
     * 팝업 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<PopupStatsResponse> getPopupStats(){
        PopupStatsResponse res = adminPopupService.getPopupStats();
        return ResponseEntity.ok(res);
    }

    /**
     * 관리자용 팝업 목록 조회 (필터링 및 검색 지원)
     */
    @GetMapping
    public ResponseEntity<Page<PopupAdminResponse>> getPopups(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable,
            @RequestParam(required = false) PopupStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword ){
        log.info("관리자 팝업 목록 조회 요청 - 페이지: {}, 상태: {}, 카테고리: {}, 키워드: {}",
                pageable.getPageNumber(), status, category, keyword);

        Page<PopupAdminResponse> popups = adminPopupService.getPopupsForAdmin(
                pageable,status,category,keyword
        );

        return ResponseEntity.ok(popups);
    }

    /**
     * 관리자용 팝업 상세 조회
     */
    @GetMapping("/{popupId}")
    public ResponseEntity<PopupAdminResponse> getPopupDetail(@PathVariable Long popupId){
        PopupAdminResponse res = adminPopupService.getPopupForAdmin(popupId);
        return ResponseEntity.ok(res);
    }

    /**
     * 팝업 상태 변경
     */
    @PutMapping("/{popupId}/status")
    public ResponseEntity<PopupAdminStatusUpdateResponse> updatePopupStatus(
            @PathVariable Long popupId,
            @Valid @RequestBody PopupStatusUpdateRequest request){
        PopupAdminStatusUpdateResponse response = adminPopupService.updatePopupStatus(popupId, request.getStatus());
        return ResponseEntity.ok(response);
    }
}