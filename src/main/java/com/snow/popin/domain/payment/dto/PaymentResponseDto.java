package com.snow.popin.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDto {

    private Boolean success;
    private String tid;
    private String redirectUrl;
    private String message;
    private String errorCode;

    /**
     * 성공 응답 생성
     */
    public static PaymentResponseDto success(String redirectUrl, String tid, String message) {
        return PaymentResponseDto.builder()
                .success(true)
                .redirectUrl(redirectUrl)
                .tid(tid)
                .message(message)
                .build();
    }
    /**
     * 실패 응답 생성
     */
    public static PaymentResponseDto failure(String message) {
        return PaymentResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }
    /**
     * 성공 여부 체크
     */
    public Boolean getSuccess() {
        return success != null ? success : false;
    }
}