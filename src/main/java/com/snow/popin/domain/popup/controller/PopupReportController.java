package com.snow.popin.domain.popup.controller;

import com.snow.popin.domain.popup.dto.request.PopupReportCreateRequest;
import com.snow.popin.domain.popup.dto.response.PopupReportResponse;
import com.snow.popin.domain.popup.entity.PopupReport;
import com.snow.popin.domain.popup.service.PopupReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/popups/reports")
@RequiredArgsConstructor
@Validated
public class PopupReportController {

    private final PopupReportService service;

    /** 제보 생성 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PopupReportResponse> create(
            @RequestPart("data") @Valid PopupReportCreateRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        PopupReport created = service.create(
                req.getBrandName(),
                req.getPopupName(),
                req.getAddress(),
                req.getStartDate(),
                req.getEndDate(),
                req.getExtraInfo(),
                images
        );

        return ResponseEntity.ok(PopupReportResponse.from(created));
    }


    /** 제보 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<PopupReport> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(service.getOne(id));
    }

    /** 상태별 목록(관리) */
    @GetMapping
    public ResponseEntity<Page<PopupReport>> byStatus(
            @RequestParam("status") PopupReport.Status status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.listByStatus(status, pageable));
    }

    /** 내 제보 목록 */
    @GetMapping("/me")
    public ResponseEntity<Page<PopupReport>> myReports(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.listMine(pageable));
    }

    /** 승인(관리) */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        service.approve(id);
        return ResponseEntity.noContent().build();
    }

    /** 반려(관리) */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        service.reject(id);
        return ResponseEntity.noContent().build();
    }
}