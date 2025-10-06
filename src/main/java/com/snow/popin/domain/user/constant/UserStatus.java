package com.snow.popin.domain.user.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {

    ACTIVE("활성"),
    INACTIVE("비활성");

    private final String description;
}
