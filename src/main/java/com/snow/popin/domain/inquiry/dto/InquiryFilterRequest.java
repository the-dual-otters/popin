package com.snow.popin.domain.inquiry.dto;

import com.snow.popin.domain.inquiry.entity.InquiryStatus;
import com.snow.popin.domain.inquiry.entity.TargetType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고 필터링 요청 DTO
 */
@Getter
@NoArgsConstructor
public class InquiryFilterRequest {

    private TargetType targetType;
    private InquiryStatus status;
    private String email;
    private Long targetId;
    private int page = 0;
    private int size = 20;

    @Builder
    public InquiryFilterRequest(TargetType targetType, InquiryStatus status,
                                String email, Long targetId, int page, int size) {
        this.targetType = targetType;
        this.status = status;
        this.email = email;
        this.targetId = targetId;
        this.page = page;
        this.size = size;
    }
}