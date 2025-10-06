package com.snow.popin.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ReservationHistoryDto {
    private Long popupId;
    private String popupTitle;
    private String category;
    private String brand;
    private LocalDate reservationDate;
    private String status;
    private String venue;
}