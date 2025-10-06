package com.snow.popin.domain.admin.controller;


import com.snow.popin.domain.inquiry.dto.InquiryCountResponse;
import com.snow.popin.domain.inquiry.dto.InquiryDetailResponse;
import com.snow.popin.domain.inquiry.dto.InquiryListResponse;
import com.snow.popin.domain.inquiry.dto.InquiryStatusUpdateRequest;
import com.snow.popin.domain.inquiry.entity.InquiryStatus;
import com.snow.popin.domain.inquiry.entity.TargetType;
import com.snow.popin.domain.admin.service.AdminInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInquiryController {

    private final AdminInquiryService inquiryAdminService;

    /**
     * 신고 목록 조회 (관리자)
     */
    @GetMapping
    public ResponseEntity<Page<InquiryListResponse>> getAllPopupInquiry(
            @RequestParam(required = false) TargetType targetType,
            @RequestParam(required = false)InquiryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<InquiryListResponse> inquiries = inquiryAdminService
                .getInquiriesForAdmin(targetType, status, page, size);

        return ResponseEntity.ok(inquiries);
    }

    /**
     * 신고 상세 조회 (관리자)
     */
    @GetMapping("/{id}")
    public ResponseEntity<InquiryDetailResponse> getInquiryDetail (@PathVariable Long id){
        InquiryDetailResponse inquiry = inquiryAdminService.getInquiry(id);
        return ResponseEntity.ok(inquiry);
    }

    /**
     * 신고 상태 변경 (관리자)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateInquiryStatus(
            @PathVariable Long id, @Valid @RequestBody InquiryStatusUpdateRequest req){
        inquiryAdminService.updateInquiryStatus(id, req);
        return ResponseEntity.ok(Map.of("message", "신고 상태가 변경되었습니다."));
    }

    /**
     * 신고 통계 조회 (관리자 대시보드)
     */
    @GetMapping("/counts")
    public ResponseEntity<InquiryCountResponse> getInquiryCounts() {
        InquiryCountResponse counts = inquiryAdminService.getInquiryCounts();
        return ResponseEntity.ok(counts);
    }

    /**
     * 이메일로 신고 조회 (관리자)
     */
    @GetMapping("/by-email")
    public ResponseEntity<Page<InquiryListResponse>> getInquiriesByEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        Page<InquiryListResponse> inquiries = inquiryAdminService.getInquiresByEmail(email, page, size);

        return ResponseEntity.ok(inquiries);
    }

    /**
     * 대기 중인 신고 개수 조회 (관리자 대시보드)
     */
    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        long count = inquiryAdminService.getPendingInquiriesCount();
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    /**
     * 특정 타입의 대기 중인 신고 개수 조회
     */
    @GetMapping("/pending-count/{targetType}")
    public ResponseEntity<Map<String, Long>> getPendingCountByType(@PathVariable TargetType targetType){
        long count = inquiryAdminService.getPendingInquiriesCountByType(targetType);
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }
}