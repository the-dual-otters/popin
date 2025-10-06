package com.snow.popin.domain.roleupgrade.controller;

import com.snow.popin.domain.roleupgrade.dto.CreateRoleUpgradeRequest;
import com.snow.popin.domain.roleupgrade.dto.RoleUpgradeResponse;
import com.snow.popin.domain.roleupgrade.service.RoleUpgradeService;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/role-upgrade")
@RequiredArgsConstructor
public class RoleUpgradeUserApiController {

    private final RoleUpgradeService roleService;
    private final UserUtil userUtil;

    /**
     * 승격 요청 생성 + files
     */
    @PostMapping("/request")
    public ResponseEntity<?> createRequest(
            @Valid @RequestPart("request") CreateRoleUpgradeRequest req,
            @RequestPart(value = "documents", required = false) List<MultipartFile> files
    ) {
        String userEmail = userUtil.getCurrentUserEmail();

        log.info("승격 요청 접수(RoleUpgradeUserApiController) - Email: {}, Role: {}, Files: {}",
                userEmail, req.getRequestedRole(), files != null ? files.size() : 0);

        Long reqId = roleService.createRoleUpgradeRequest(userEmail, req, files);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "역할 승격 요청이 성공적으로 제출되었습니다.",
                "requestId", reqId
        ));
    }

    /**
     *  내 승격 요청 목록 조회
     */
    @GetMapping("/my-requests")
    public ResponseEntity<List<RoleUpgradeResponse>> getMyRequests(){
        String userEmail = userUtil.getCurrentUserEmail();
        List<RoleUpgradeResponse> requests = roleService.getMyRoleUpgradeRequests(userEmail);

        return ResponseEntity.ok(requests);
    }

}
