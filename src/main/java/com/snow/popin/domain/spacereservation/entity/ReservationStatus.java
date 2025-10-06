package com.snow.popin.domain.spacereservation.entity;

public enum ReservationStatus {
    PENDING("대기중"),
    ACCEPTED("승인됨"),
    REJECTED("거절됨"),
    CANCELLED("취소됨");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}