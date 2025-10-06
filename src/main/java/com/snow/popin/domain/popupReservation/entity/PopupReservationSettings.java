package com.snow.popin.domain.popupReservation.entity;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.global.common.BaseEntity;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "popup_reservation_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PopupReservationSettings extends BaseEntity {

    @Id
    private Long popupId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // popupId가 PK이면서 FK 역할
    @JoinColumn(name = "popup_id")
    private Popup popup;

    @Column(name = "max_capacity_per_slot", nullable = false)
    @Builder.Default
    private Integer maxCapacityPerSlot = 10;

    @Column(name = "time_slot_interval", nullable = false)
    @Builder.Default
    private Integer timeSlotInterval = 30;

    @Column(name = "advance_booking_days")
    @Builder.Default
    private Integer advanceBookingDays = 30;

    @Column(name = "cancellation_deadline_hours")
    @Builder.Default
    private Integer cancellationDeadlineHours = 24;

    @Column(name = "max_party_size")
    @Builder.Default
    private Integer maxPartySize = 10;

    @Column(name = "allow_same_day_booking")
    @Builder.Default
    private Boolean allowSameDayBooking = true;

    // 비즈니스 메서드
    public void updateBasicSettings(Integer maxCapacity, Integer timeInterval) {
        if (maxCapacity != null && maxCapacity > 0) {
            this.maxCapacityPerSlot = maxCapacity;
        }
        if (timeInterval != null && timeInterval >= 15) {
            this.timeSlotInterval = timeInterval;
        }
    }

    // 예약 가능 여부 검증
    public boolean isValidPartySize(int partySize) {
        return partySize > 0 && partySize <= this.maxPartySize;
    }

    // 팩토리 메서드
    public static PopupReservationSettings createDefault(Popup popup) {
        return PopupReservationSettings.builder()
                .popup(popup)
                .build();
    }
    public void setTimeSlotInterval(Integer interval) {
        this.timeSlotInterval = interval;
    }

    public void setMaxCapacityPerSlot(Integer capacity) {
        this.maxCapacityPerSlot = capacity;
    }

    public void setMaxPartySize(Integer size) {
        this.maxPartySize = size;
    }

    public void setAdvanceBookingDays(Integer days) {
        this.advanceBookingDays = days;
    }

    public void setAllowSameDayBooking(Boolean allow) {
        this.allowSameDayBooking = allow;
    }

}