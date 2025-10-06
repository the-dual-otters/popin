package com.snow.popin.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Getter
@NoArgsConstructor
@Builder
public class LogoutRequest {

    private String accessToken;
    private String refreshToken; // 향후 확장용

    // @Builder와 함께 사용할 명시적 생성자
    @Builder
    public LogoutRequest(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}