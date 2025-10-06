package com.snow.popin.domain.popupReservation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
public class AvailableSlotDto {
    private LocalTime startTime;
    private LocalTime endTime;
    private int remainingCapacity;

    public static AvailableSlotDto of(LocalTime start, LocalTime end, int remaining) {
        return new AvailableSlotDto(start, end, remaining);
    }
}
