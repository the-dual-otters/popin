package com.snow.popin.domain.popupReservation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class TimeSlotDto {
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String timeRangeText;
    private final boolean available;
    private final int remainingSlots; // 남은 예약 가능 슬롯 수
    private int maxCapacity;
    private int currentReservations;
    private Double utilizationRate;

    public static class TimeSlotDtoBuilder {
        public TimeSlotDto build() {
            if (timeRangeText == null && startTime != null && endTime != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                this.timeRangeText = startTime.format(formatter) + " - " + endTime.format(formatter);
            }

            if (utilizationRate == null && maxCapacity > 0) {
                this.utilizationRate = (double) currentReservations / maxCapacity;
            }

            return new TimeSlotDto(startTime, endTime, timeRangeText, available,
                    remainingSlots, maxCapacity, currentReservations, utilizationRate);
        }
    }

    public static TimeSlotDto createAvailable(LocalTime startTime, LocalTime endTime,
                                              int currentReservations, int maxCapacity) {
        int remaining = maxCapacity - currentReservations;
        return TimeSlotDto.builder()
                .startTime(startTime)
                .endTime(endTime)
                .available(remaining > 0)
                .remainingSlots(Math.max(0, remaining))
                .maxCapacity(maxCapacity)
                .currentReservations(currentReservations)
                .build();
    }

    public static TimeSlotDto createUnavailable(LocalTime startTime, LocalTime endTime, String reason) {
        return TimeSlotDto.builder()
                .startTime(startTime)
                .endTime(endTime)
                .available(false)
                .remainingSlots(0)
                .maxCapacity(0)
                .currentReservations(0)
                .build();
    }

    public static TimeSlotDto of(LocalTime startTime, LocalTime endTime, int currentReservations, int maxCapacity) {
        int remaining = maxCapacity - currentReservations;
        return TimeSlotDto.builder()
                .startTime(startTime)
                .endTime(endTime)
                .available(remaining > 0)
                .remainingSlots(Math.max(0, remaining))
                .build();
    }

    //  메서드
    public boolean canAccommodate(int partySize) {
        return available && remainingSlots >= partySize;
    }
}