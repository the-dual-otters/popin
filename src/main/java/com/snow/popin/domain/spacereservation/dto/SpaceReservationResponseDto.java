package com.snow.popin.domain.spacereservation.dto;

import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import com.snow.popin.domain.spacereservation.entity.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SpaceReservationResponseDto {
    private Long id;

    private Long popupId;
    private String popupTitle;
    private String brandName;
    private String popupMainImage;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String message;
    private String contactPhone;
    private ReservationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private SpaceInfo space;

    @Getter
    @Builder
    public static class SpaceInfo {
        private Long id;
        private String title;
        private String address;
        private Integer rentalFee;
        private String coverImageUrl;
    }

    public static SpaceReservationResponseDto from(SpaceReservation reservation) {
        return SpaceReservationResponseDto.builder()
                .id(reservation.getId())
                .popupId(reservation.getPopup().getId())
                .popupTitle(reservation.getPopup().getTitle())
                .brandName(reservation.getBrand().getName())
                .popupMainImage(reservation.getPopup().getMainImageUrl())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .message(reservation.getMessage())
                .contactPhone(reservation.getContactPhone())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .space(SpaceInfo.builder()
                        .id(reservation.getSpace().getId())
                        .title(reservation.getSpace().getTitle())
                        .address(reservation.getSpace().getAddress())
                        .rentalFee(reservation.getSpace().getRentalFee())
                        .coverImageUrl(reservation.getSpace().getCoverImageUrl())
                        .build())
                .build();
    }
}
