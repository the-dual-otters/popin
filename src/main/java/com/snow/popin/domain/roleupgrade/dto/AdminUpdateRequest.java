package com.snow.popin.domain.roleupgrade.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class AdminUpdateRequest {

    @NotNull(message = "승인 상태는 필수입니다")
    private Boolean approve;

    private String rejectReason; // 반려 시에만 필요

    @Builder
    public AdminUpdateRequest(Boolean approve, String rejectReason) {
        this.approve = approve;
        this.rejectReason = rejectReason;

    }

    public boolean isApprove(){
        return approve != null && approve;
    }

    // 반려 시 반려 사유 필수
    @AssertTrue(message = "반려 시 반려 사유를 입력해주세요.")
    private boolean isRejectReasonValid(){
        if (approve != null && !approve){
            return rejectReason != null && !rejectReason.trim().isEmpty();
        }
        return true;
    }
}
