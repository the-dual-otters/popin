package com.snow.popin.domain.admin.controller;

import com.snow.popin.domain.admin.service.AdminRoleUpgradeService;
import com.snow.popin.domain.roleupgrade.dto.AdminUpdateRequest;
import com.snow.popin.domain.roleupgrade.dto.RoleUpgradeResponse;
import com.snow.popin.domain.roleupgrade.entity.ApprovalStatus;
import com.snow.popin.domain.user.constant.Role;
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
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/role-upgrade")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleUpgradeApiController {

    private final AdminRoleUpgradeService adminRoleService;

    /**
     * 전체 역할 승격 요청 조회 (필터링 지원)
     */
    @GetMapping("/requests")
    public ResponseEntity<Page<RoleUpgradeResponse>> getAllRequests(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("관리자 역할 승격 요청 조회 - status: {}, role: {}, page: {}", status, role, pageable.getPageNumber());


        Page<RoleUpgradeResponse> reqs;

        if (status != null && role != null) {
            // 상태와 역할 모두 필터링
            reqs = adminRoleService.getRoleUpgradeRequestsByStatusAndRole(status, role, pageable);
        } else if (status != null) {
            // 상태만 필터링
            reqs = adminRoleService.getRoleUpgradeRequestsByStatus(status, pageable);
        } else if (role != null) {
            // 역할만 필터링
            reqs = adminRoleService.getRoleUpgradeRequestsByRole(role, pageable);
        } else {
            // 전체 조회
            reqs = adminRoleService.getAllRoleUpgradeRequests(pageable);
        }

        return ResponseEntity.ok(reqs);
    }

    /**
     * 역할 승격 요청 상세 조회
     */
    @GetMapping("/requests/{id}")
    public ResponseEntity<RoleUpgradeResponse> getRequestDetail(@PathVariable Long id) {
        log.info("관리자 역할 승격 요청 상세 조회 - id: {}", id);

        RoleUpgradeResponse res = adminRoleService.getRoleUpgradeRequestForAdmin(id);
        return ResponseEntity.ok(res);
    }

    /**
     * 역할 승격 요청 처리 (승인/거절)
     */
    @PutMapping("/requests/{id}/process")
    public ResponseEntity<Map<String, String>> processRequest(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateRequest req) {
        log.info("관리자 역할 승격 요청 처리 - id: {}, approve: {}", id, req.isApprove());

        adminRoleService.processRoleUpgradeRequest(id, req);

        String msg = req.isApprove() ?
                "역할 승격 요청이 승인되었습니다." : "역할 승격 요청이 거절되었습니다.";

        return ResponseEntity.ok(Map.of("message", msg));
    }

    /**
     * 대기 중인 승격 요청 개수 조회 (통계용)
     */
    @GetMapping("/pending-count")
    public ResponseEntity<Long> getPendingCount(){
        long count = adminRoleService.getPendingRequestCount();
        return ResponseEntity.of(java.util.Optional.of(count));
    }

    /**
     * 승격 요청 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUpgradeStats() {
        log.info("승격 요청 통계 조회");

        Map<String, Object> stats = Map.of(
                "pendingCount", adminRoleService.getPendingRequestCount(),
                "totalCount", adminRoleService.getTotalRequestCount(),
                "approvedCount", adminRoleService.getApprovedRequestCount(),
                "rejectedCount", adminRoleService.getRejectedRequestCount()
        );

        return ResponseEntity.ok(stats);
    }
}