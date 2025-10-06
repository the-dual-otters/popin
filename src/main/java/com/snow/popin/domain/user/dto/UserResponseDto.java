package com.snow.popin.domain.user.dto;

import com.snow.popin.domain.user.entity.User;
import lombok.Getter;

@Getter
public class UserResponseDto {
    private final Long id;
    private final String email;
    private final String name;
    private final String nickname;
    private final String phone;
    private final String role;

    public UserResponseDto(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.nickname = user.getNickname();
        this.phone = user.getPhone();
        this.role = user.getRole().name();
    }
}
