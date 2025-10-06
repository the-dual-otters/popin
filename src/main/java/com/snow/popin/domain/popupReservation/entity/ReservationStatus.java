package com.snow.popin.domain.popupReservation.entity;

public enum ReservationStatus {
    RESERVED("예약됨"),
    CANCELLED("예약취소"),
    VISITED("방문완료");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
