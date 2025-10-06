package com.snow.popin.domain.inquiry.dto;

import com.snow.popin.domain.inquiry.entity.InquiryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 신고 상태 변경 요청 DTO
 */
@Getter
@NoArgsConstructor
public class InquiryStatusUpdateRequest {
    @NotNull(message = "상태는 필수입니다")
    private InquiryStatus status;

    @Builder
    public InquiryStatusUpdateRequest(InquiryStatus status) {
        this.status = status;
    }

}
