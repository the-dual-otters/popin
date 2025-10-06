package com.snow.popin.domain.mypage.host.entity;

public enum HostRole {
    OWNER("소유자"),
    MANAGER("관리자"),
    MEMBER("멤버");

    private final String description;

    HostRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}