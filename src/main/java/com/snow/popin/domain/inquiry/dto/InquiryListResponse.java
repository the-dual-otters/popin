package com.snow.popin.domain.inquiry.dto;

import com.snow.popin.domain.inquiry.entity.Inquiry;
import com.snow.popin.domain.inquiry.entity.InquiryStatus;
import com.snow.popin.domain.inquiry.entity.TargetType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신고 목록 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
public class InquiryListResponse {

    private Long id;
    private String email;
    private TargetType targetType;
    private Long targetId;
    private String subject;
    private InquiryStatus status;
    private LocalDateTime createdAt;
    private String targetTitle;

    @Builder
    public InquiryListResponse(Long id, String email, TargetType targetType,
                               Long targetId, String subject, InquiryStatus status,
                               LocalDateTime createdAt, String targetTitle) {
        this.id = id;
        this.email = email;
        this.targetType = targetType;
        this.targetId = targetId;
        this.subject = subject;
        this.status = status;
        this.createdAt = createdAt;
        this.targetTitle = targetTitle;
    }

    /**
     * Inquiry 엔티티를 ListResponse DTO로 변환
     * @param inquiry 신고 엔티티
     * @param targetTitle 신고 대상 제목 (외부에서 조회)
     * @return InquiryListResponse
     */
    public static InquiryListResponse from(Inquiry inquiry, String targetTitle){
        return InquiryListResponse.builder()
                .id(inquiry.getId())
                .email(inquiry.getEmail())
                .targetType(inquiry.getTargetType())
                .targetId(inquiry.getTargetId())
                .subject(inquiry.getSubject())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .targetTitle(targetTitle)
                .build();
    }

    /**
     * Inquiry 엔티티를 ListResponse DTO로 변환 (targetTitle 없이)
     * @param inquiry 신고 엔티티
     * @return InquiryListResponse
     */
    public static InquiryListResponse from(Inquiry inquiry) {
        return from(inquiry, "조회 중...");
    }

}
