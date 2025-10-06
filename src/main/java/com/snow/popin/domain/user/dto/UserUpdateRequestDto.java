package com.snow.popin.domain.user.dto;

import lombok.Getter;

@Getter
public class UserUpdateRequestDto {
    private final String name;
    private final String nickname;
    private final String phone;

    public UserUpdateRequestDto(String name, String nickname, String phone) {
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
    }
}
