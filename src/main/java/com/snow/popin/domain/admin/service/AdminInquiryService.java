package com.snow.popin.domain.admin.service;

import com.snow.popin.domain.inquiry.dto.InquiryCountResponse;
import com.snow.popin.domain.inquiry.dto.InquiryDetailResponse;
import com.snow.popin.domain.inquiry.dto.InquiryListResponse;
import com.snow.popin.domain.inquiry.dto.InquiryStatusUpdateRequest;
import com.snow.popin.domain.inquiry.entity.Inquiry;
import com.snow.popin.domain.inquiry.entity.InquiryStatus;
import com.snow.popin.domain.inquiry.entity.TargetType;
import com.snow.popin.domain.inquiry.repository.InquiryRepository;
import com.snow.popin.domain.inquiry.service.InquiryService;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자용 신고 서비스
 * 신고 관리, 상태 변경, 통계 조회 등의 관리자 전용 기능 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminInquiryService {

    private final InquiryRepository inquiryRepo;
    private final InquiryService inquiryService;

    /**
     * 신고 목록 조회 (관리자용 - 필터링 지원)
     */
    public Page<InquiryListResponse> getInquiriesForAdmin(
            TargetType targetType, InquiryStatus status, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Inquiry> inquiries;
        if (targetType != null && status != null){
            inquiries = inquiryRepo.findByTargetTypeAndStatusOrderByCreatedAtDesc(targetType, status, pageable);
        } else if (targetType != null){
            inquiries = inquiryRepo.findByTargetTypeOrderByCreatedAtDesc(targetType, pageable);
        } else if (status != null){
            inquiries = inquiryRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            inquiries = inquiryRepo.findAll(pageable);
        }

        return inquiries.map(inquiry -> InquiryListResponse.from(inquiry,
                inquiryService.getTargetTitle(inquiry.getTargetType(), inquiry.getTargetId())));
    }

    /**
     * 신고 상세 조회 (관리자용)
     */
    public InquiryDetailResponse getInquiry(Long id){
        Inquiry inquiry = inquiryRepo.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND,"신고를 찾을 수 없습니다."));

        return InquiryDetailResponse.from(inquiry, inquiryService.getTargetTitle(inquiry.getTargetType(), inquiry.getTargetId()));
    }

    /**
     * 신고 상태 변경 (관리자용)
     */
    @Transactional
    public void updateInquiryStatus(Long id, InquiryStatusUpdateRequest req){
        Inquiry inquiry = inquiryRepo.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND,"신고를 찾을 수 없습니다."));

        inquiry.updateStatus(req.getStatus());
        log.info("신고 상태가 변경되었습니다. ID: {}, 상태: {}", id, req.getStatus());
    }

    /**
     * 신고 통계 조회 (관리자용)
     */
    public InquiryCountResponse getInquiryCounts() {
        long total = inquiryRepo.count();
        long popup = inquiryRepo.countByTargetType(TargetType.POPUP);
        long review = inquiryRepo.countByTargetType(TargetType.REVIEW);
        long user = inquiryRepo.countByTargetType(TargetType.USER);
        long space = inquiryRepo.countByTargetType(TargetType.SPACE);
        long general = inquiryRepo.countByTargetType(TargetType.GENERAL);
        long open = inquiryRepo.countByStatus(InquiryStatus.OPEN);
        long inProgress = inquiryRepo.countByStatus(InquiryStatus.IN_PROGRESS);
        long closed = inquiryRepo.countByStatus(InquiryStatus.CLOSED);
        long popupPending = inquiryRepo.countByTargetTypeAndStatus(TargetType.POPUP, InquiryStatus.OPEN);
        long spacePending = inquiryRepo.countByTargetTypeAndStatus(TargetType.SPACE, InquiryStatus.OPEN);
        long reviewPending = inquiryRepo.countByTargetTypeAndStatus(TargetType.REVIEW, InquiryStatus.OPEN);
        long generalPending = inquiryRepo.countByTargetTypeAndStatus(TargetType.GENERAL, InquiryStatus.OPEN);
        long userPending = inquiryRepo.countByTargetTypeAndStatus(TargetType.USER, InquiryStatus.OPEN);

        return InquiryCountResponse.builder()
                .total(total)
                .popup(popup)
                .review(review)
                .user(user)
                .space(space)
                .general(general)
                .open(open)
                .inProgress(inProgress)
                .closed(closed)
                .popupPending(popupPending)
                .spacePending(spacePending)
                .reviewPending(reviewPending)
                .generalPending(generalPending)
                .userPending(userPending)
                .build();
    }


    /**
     * 이메일로 신고 조회 (관리자용)
     */
    public Page<InquiryListResponse> getInquiresByEmail(String email, int page, int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Inquiry> inquiries = inquiryRepo.findByEmailOrderByCreatedAtDesc(email, pageable);

        return inquiries.map(inquiry -> InquiryListResponse.from(inquiry,
                inquiryService.getTargetTitle(inquiry.getTargetType(), inquiry.getTargetId())));
    }

    /**
     * 대기 중인 신고 총 개수 조회
     */
    public long getPendingInquiriesCount(){
        return inquiryRepo.countByStatus(InquiryStatus.OPEN);
    }

    /**
     * 특정 타입의 대기 중인 신고 개수 조회
     */
    public long getPendingInquiriesCountByType(TargetType targetType){
        return inquiryRepo.countByTargetTypeAndStatus(targetType, InquiryStatus.OPEN);
    }


}