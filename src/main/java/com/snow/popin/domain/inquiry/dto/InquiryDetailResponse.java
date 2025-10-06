package com.snow.popin.domain.inquiry.dto;

import com.snow.popin.domain.inquiry.entity.Inquiry;
import com.snow.popin.domain.inquiry.entity.InquiryStatus;
import com.snow.popin.domain.inquiry.entity.TargetType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신고 상세 조회 응답 DTO
 */
@Getter
@NoArgsConstructor
public class InquiryDetailResponse {

    private Long id;
    private String email;
    private TargetType targetType;
    private Long targetId;
    private String subject;
    private String content;
    private InquiryStatus status;
    private LocalDateTime createdAt;
    private String targetTitle;

    @Builder
    public InquiryDetailResponse(Long id, String email, TargetType targetType,
                                 Long targetId, String subject, String content,
                                 InquiryStatus status, LocalDateTime createdAt,
                                 String targetTitle) {
        this.id = id;
        this.email = email;
        this.targetType = targetType;
        this.targetId = targetId;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
        this.targetTitle = targetTitle;
    }

    /**
     * Inquiry 엔티티를 DetailResponse DTO로 변환
     * @param inquiry 신고 엔티티
     * @param targetTitle 신고 대상 제목 (외부에서 조회)
     * @return InquiryDetailResponse
     */
    public static InquiryDetailResponse from(Inquiry inquiry, String targetTitle) {
        return InquiryDetailResponse.builder()
                .id(inquiry.getId())
                .email(inquiry.getEmail())
                .targetType(inquiry.getTargetType())
                .targetId(inquiry.getTargetId())
                .subject(inquiry.getSubject())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .targetTitle(targetTitle)
                .build();
    }

    /**
     * Inquiry 엔티티를 DetailResponse DTO로 변환 (targetTitle 없이)
     * @param inquiry 신고 엔티티
     * @return InquiryDetailResponse
     */
    public static InquiryDetailResponse from(Inquiry inquiry) {
        return from(inquiry, "조회 중...");
    }

}
