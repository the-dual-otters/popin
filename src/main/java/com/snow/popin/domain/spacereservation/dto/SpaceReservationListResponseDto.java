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
public class SpaceReservationListResponseDto {
    private Long id;

    private Long popupId;
    private String popupTitle;
    private String brandName;
    private String popupMainImage;
    private Long spaceId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private ReservationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // 공간 정보
    private String spaceTitle;
    private String spaceAddress;
    private String spaceImageUrl;

    // 예약자 정보
    private String hostName;
    private String hostPhone;

    public static SpaceReservationListResponseDto fromForHost(SpaceReservation reservation) {
        return SpaceReservationListResponseDto.builder()
                .id(reservation.getId())
                .popupId(reservation.getPopup().getId())
                .popupTitle(reservation.getPopup().getTitle())
                .brandName(reservation.getBrand().getName())
                .popupMainImage(reservation.getPopup().getMainImageUrl())
                .spaceId(reservation.getSpace().getId())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .spaceTitle(reservation.getSpace().getTitle())
                .spaceAddress(reservation.getSpace().getVenue() != null ?
                        reservation.getSpace().getVenue().getFullAddress() :
                        (reservation.getSpace().getAddress() != null ? reservation.getSpace().getAddress() : "주소 정보 없음"))
                .spaceImageUrl(reservation.getSpace().getCoverImageUrl())
                .build();
    }

    public static SpaceReservationListResponseDto fromForProvider(SpaceReservation reservation) {
        return SpaceReservationListResponseDto.builder()
                .id(reservation.getId())
                .popupId(reservation.getPopup().getId())
                .popupTitle(reservation.getPopup().getTitle())
                .brandName(reservation.getBrand().getName())
                .popupMainImage(reservation.getPopup().getMainImageUrl())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .spaceTitle(reservation.getSpace().getTitle())
                .spaceAddress(reservation.getSpace().getAddress())
                .spaceImageUrl(reservation.getSpace().getCoverImageUrl())
                .hostName(reservation.getHost().getName())
                .hostPhone(reservation.getContactPhone() != null ? reservation.getContactPhone() : reservation.getHost().getPhone())
                .build();
    }
}
