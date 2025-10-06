package com.snow.popin.domain.user.dto;

import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.constant.UserStatus;
import com.snow.popin.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserSearchResponse {

    private Long userId;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static UserSearchResponse from(User user) {
        if (user == null) {
            return null;
        }

        return UserSearchResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
