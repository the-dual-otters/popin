package com.snow.popin.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class LogoutResponse {

    private String message;
    private boolean success;

    // @Builder와 함께 사용할 명시적 생성자
    @Builder
    public LogoutResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public static LogoutResponse success(String message){
        return LogoutResponse.builder()
                .message(message)
                .success(true)
                .build();
    }

    public static LogoutResponse failure(String message){
        return LogoutResponse.builder()
                .message(message)
                .success(false)
                .build();
    }
}