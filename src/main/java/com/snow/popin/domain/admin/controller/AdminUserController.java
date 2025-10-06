package com.snow.popin.domain.admin.controller;

import com.snow.popin.domain.admin.service.AdminUserService;
import com.snow.popin.domain.admin.service.AdminRoleUpgradeService;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.dto.UserDetailResponse;
import com.snow.popin.domain.user.dto.UserSearchResponse;
import com.snow.popin.domain.user.dto.UserStatusUpdateRequest;
import com.snow.popin.domain.user.dto.UserStatusUpdateResponse;
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

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AdminRoleUpgradeService adminRoleService;

    /**
     * 회원 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserSearchResponse>> searchUsers(
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable){
        log.info("회원 검색 요청 Controller - searchType: {}, keyword: {}, role: {}, page: {}",
                searchType, keyword, role, pageable.getPageNumber());

        Page<UserSearchResponse> users = adminUserService.searchUser(searchType, keyword, role, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * 회원 상세 정보 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> getUserDetail(@PathVariable Long userId){
        log.info("회원 상세 정보 조회 요청 - userId: {}", userId);

        UserDetailResponse res = adminUserService.getUserDetailById(userId);
        return ResponseEntity.ok(res);
    }

    /**
     * 사용자 상태 변경
     */
    @PutMapping("/{userId}/status")
    public ResponseEntity<UserStatusUpdateResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request){
        UserStatusUpdateResponse response = adminUserService.updateUserStatus(userId, request.getStatus());
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 회원 수 조회
     */
            @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTotalUserCount(){
        log.info("전체 회원 수 조회 요청");

        Long total = adminUserService.getTotalUserCount();
        return ResponseEntity.ok(Map.of("totalCount", total));
    }

    /**
     * 역할별 회원 수 조회
     */
    @GetMapping("/count/by-role")
    public ResponseEntity<Map<String, Long>> getUserCountByRole(){
        log.info("역할별 회원 수 조회 요청");

        Map<String, Long> roleState = adminUserService.getUserCountByRole();
        return ResponseEntity.ok(roleState);
    }

    /**
     * 계정 전환 요청 개수 조회 (프론트엔드 호환성을 위해 추가)
     */
    @GetMapping("/upgrade-requests/count")
    public ResponseEntity<Long> getUpgradeRequestCount() {
        log.info("계정 전환 요청 개수 조회");

        Long count = adminRoleService.getPendingRequestCount();
        return ResponseEntity.ok(count);
    }
}