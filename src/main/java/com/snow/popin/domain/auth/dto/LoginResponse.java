package com.snow.popin.domain.auth.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String name;
    private String role;

    // @Builder와 함께 사용할 명시적 생성자
    @Builder
    public LoginResponse(String accessToken, String tokenType, Long userId,
                         String email, String name, String role) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public static LoginResponse of(String accessToken, Long userId, String email, String name, String role) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(userId)
                .email(email)
                .name(name)
                .role(role)
                .build();
    }
}