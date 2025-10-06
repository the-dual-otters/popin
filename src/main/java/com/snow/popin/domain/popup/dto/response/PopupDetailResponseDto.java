package com.snow.popin.domain.popup.dto.response;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.entity.Tag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PopupDetailResponseDto {
    private final Long id;
    private final String title;
    private final String summary;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String periodText;
    private final PopupStatus status;
    private final String statusDisplayText;
    private final String mainImageUrl;
    private final Boolean isFeatured;
    private final Boolean reservationAvailable;
    private final Boolean waitlistAvailable;

    private final Integer entryFee;
    private final Boolean isFreeEntry;
    private final String feeDisplayText;

    private final Long viewCount;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private final String venueName;
    private final String venueAddress;
    private final String region;
    private Double latitude;
    private Double longitude;
    private final Boolean parkingAvailable;

    private final Long categoryId;
    private final String categoryName;
    private final String categorySlug;

    private List<PopupImageResponseDto> images;
    private List<PopupHoursResponseDto> hours;
    private final List<String> tags;

    public static PopupDetailResponseDto from(Popup popup) {
        return PopupDetailResponseDto.builder()
                .id(popup.getId())
                .title(popup.getTitle())
                .summary(popup.getSummary())
                .description(popup.getDescription())
                .startDate(popup.getStartDate())
                .endDate(popup.getEndDate())
                .periodText(popup.getPeriodText())
                .status(popup.getStatus())
                .statusDisplayText(getStatusDisplayText(popup.getStatus()))
                .mainImageUrl(popup.getMainImageUrl())
                .isFeatured(popup.getIsFeatured())
                .reservationAvailable(popup.getReservationAvailable())
                .waitlistAvailable(popup.getWaitlistAvailable())
                .entryFee(popup.getEntryFee())
                .isFreeEntry(popup.isFreeEntry())
                .feeDisplayText(popup.getFeeDisplayText())
                .viewCount(popup.getViewCount())
                .createdAt(popup.getCreatedAt())
                .updatedAt(popup.getUpdatedAt())
                .venueName(popup.getVenueName())
                .venueAddress(popup.getVenueAddress())
                .region(popup.getRegion())
                .latitude(popup.getLatitude())
                .longitude(popup.getLongitude())
                .parkingAvailable(popup.getParkingAvailable())
                .categoryId(popup.getCategory() != null ? popup.getCategory().getId() : null)
                .categoryName(popup.getCategoryName())
                .categorySlug(popup.getCategorySlug())
                .images(popup.getImages().stream()
                        .map(PopupImageResponseDto::from)
                        .collect(Collectors.toList()))
                .hours(popup.getHours().stream()
                        .map(PopupHoursResponseDto::from)
                        .collect(Collectors.toList()))
                .tags(popup.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toList()))
                .build();
    }

    private static String getStatusDisplayText(PopupStatus status) {
        switch (status) {
            case ONGOING: return "진행 중";
            case PLANNED: return "오픈 예정";
            case ENDED: return "종료";
            default: return status.name();
        }
    }
}