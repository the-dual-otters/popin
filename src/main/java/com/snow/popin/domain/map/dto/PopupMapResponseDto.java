package com.snow.popin.domain.map.dto;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopupMapResponseDto {
    private Long id;
    private String title;
    private String summary;
    private String period;
    private PopupStatus status;
    private String statusDisplayText;
    private String mainImageUrl;
    private Boolean isFreeEntry;
    private String feeDisplayText;
    private Boolean reservationAvailable;
    private Boolean waitlistAvailable;

    // 지도 표시용 위치 정보
    private Double latitude;
    private Double longitude;
    private String venueName;
    private String venueAddress;
    private String region;
    private Boolean parkingAvailable;

    // 카테고리 정보
    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    public static PopupMapResponseDto from(Popup popup) {
        return PopupMapResponseDto.builder()
                .id(popup.getId())
                .title(popup.getTitle())
                .summary(popup.getSummary())
                .period(popup.getPeriodText())
                .status(popup.getStatus())
                .statusDisplayText(getStatusDisplayText(popup.getStatus()))
                .mainImageUrl(popup.getMainImageUrl())
                .isFreeEntry(popup.isFreeEntry())
                .feeDisplayText(popup.getFeeDisplayText())
                .reservationAvailable(popup.getReservationAvailable())
                .waitlistAvailable(popup.getWaitlistAvailable())
                .latitude(popup.getLatitude())
                .longitude(popup.getLongitude())
                .venueName(popup.getVenueName())
                .venueAddress(popup.getVenueAddress())
                .region(popup.getRegion())
                .parkingAvailable(popup.getParkingAvailable())
                .categoryId(popup.getCategory() != null ? popup.getCategory().getId() : null)
                .categoryName(popup.getCategoryName())
                .categorySlug(popup.getCategorySlug())
                .build();
    }

    // 좌표가 유효한지 확인
    public boolean hasValidCoordinates() {
        return latitude != null && longitude != null
                && latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    // 거리 계산용 (하버사인 공식)
    public double getDistanceTo(double lat, double lng) {
        if (!hasValidCoordinates()) {
            return Double.MAX_VALUE;
        }

        final int EARTH_RADIUS = 6371;

        double latDistance = Math.toRadians(lat - this.latitude);
        double lngDistance = Math.toRadians(lng - this.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    private static String getStatusDisplayText(PopupStatus status) {
        if (status == null) return "상태 미정";
        switch (status) {
            case ONGOING: return "진행 중";
            case PLANNED: return "오픈 예정";
            case ENDED: return "종료";
            default: return status.name();
        }
    }
}