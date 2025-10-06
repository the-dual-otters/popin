package com.snow.popin.domain.auth.dto;

import lombok.*;

@Getter
@NoArgsConstructor
public class SignupResponse {

    private boolean success;
    private String message;
    private String email;
    private String name;
    private String nickname;
    private String phone;

    // @Builder를 사용할 때 필요한 생성자를 명시적으로 정의
    @Builder
    public SignupResponse(boolean success, String message, String email, String name, String nickname, String phone) {
        this.success = success;
        this.message = message;
        this.email = email;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
    }

    public static SignupResponse success(String email, String name, String nickname) {
        return SignupResponse.builder()
                .success(true)
                .message("회원가입이 완료되었습니다.")
                .email(email)
                .name(name)
                .nickname(nickname)
                .build();
    }

    public static SignupResponse failure(String message) {
        return SignupResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}