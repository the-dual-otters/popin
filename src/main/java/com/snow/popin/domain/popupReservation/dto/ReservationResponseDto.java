package com.snow.popin.domain.popupReservation.dto;

import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 예약 응답 DTO
 *
 * 예약 정보와 함께 팝업 정보, 상태, 추가 메타 정보를 포함
 */
@Getter
@Builder
public class ReservationResponseDto {
    private Long id;
    private Long popupId;
    private String popupTitle;
    private String popupSummary;
    private String venueName;
    private String venueAddress;
    private String name;
    private String phone;
    private Integer partySize;
    private LocalDateTime reservationDate;
    private LocalDateTime reservedAt;
    private ReservationStatus status;
    private String statusDescription;

    private boolean canCancel;
    private boolean isUpcoming;
    private Long hoursUntilReservation;
    private String timeUntilReservation;

    public static ReservationResponseDto from(Reservation reservation) {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationTime = reservation.getReservationDate();

        boolean isUpcoming = reservationTime != null && reservationTime.isAfter(now);
        long hoursUntil = (isUpcoming && reservationTime != null) ?
                ChronoUnit.HOURS.between(now, reservationTime) : 0;

        var builder = ReservationResponseDto.builder()
                .id(reservation.getId())
                .name(reservation.getName())
                .phone(reservation.getPhone())
                .partySize(reservation.getPartySize())
                .reservationDate(reservation.getReservationDate())
                .reservedAt(reservation.getReservedAt())
                .status(reservation.getStatus())
                .statusDescription(reservation.getStatus() != null ? reservation.getStatus().getDescription() : "")
                .canCancel(reservation.canCancel() && isUpcoming)
                .isUpcoming(isUpcoming)
                .hoursUntilReservation(hoursUntil)
                .timeUntilReservation(formatTimeUntil(hoursUntil));

        if (reservation.getPopup() != null) {
            builder.popupId(reservation.getPopup().getId())
                    .popupTitle(reservation.getPopup().getTitle())
                    .popupSummary(reservation.getPopup().getSummary())
                    .venueName(reservation.getPopup().getVenueName())
                    .venueAddress(reservation.getPopup().getVenueAddress());
        }

        return builder.build();
    }

    private static String formatTimeUntil(long hours) {
        if (hours <= 0) return "지난 예약";
        if (hours < 24) return hours + "시간 후";
        long days = hours / 24;
        long remainingHours = hours % 24;
        return days + "일 " + remainingHours + "시간 후";
    }
}