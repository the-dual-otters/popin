package com.snow.popin.domain.popup.dto.response;

import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PopupSummaryResponseDto {
    private Long id;
    private String title;
    private String summary;
    private String period;
    private PopupStatus status;
    private String mainImageUrl;
    private Boolean isFeatured;
    private Boolean reservationAvailable;
    private Boolean waitlistAvailable;

    private Integer entryFee;
    private Boolean isFreeEntry;
    private String feeDisplayText;
    private Long viewCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PopupImageResponseDto> images;

    private String venueName;
    private String venueAddress;
    private String region;
    private Boolean parkingAvailable;

    // 카테고리 정보
    private Long categoryId;
    private String categoryName;
    private String categorySlug;

    // 브랜드 정보
    private Long brandId;
    private String brandName;

    public static PopupSummaryResponseDto from(Popup popup) {
        return PopupSummaryResponseDto.builder()
                .id(popup.getId())
                .title(popup.getTitle())
                .summary(popup.getSummary())
                .period(popup.getPeriodText())
                .status(popup.getStatus())
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
                .images(popup.getImages().stream()
                        .map(PopupImageResponseDto::from)
                        .collect(Collectors.toList()))
                .venueName(popup.getVenueName())
                .venueAddress(popup.getVenueAddress())
                .region(popup.getRegion())
                .parkingAvailable(popup.getParkingAvailable())
                .categoryId(popup.getCategory() != null ? popup.getCategory().getId() : null)
                .categoryName(popup.getCategoryName())
                .categorySlug(popup.getCategorySlug())
                .brandId(popup.getBrandId())
                .brandName(null)
                .build();
    }

    public static PopupSummaryResponseDto fromWithBrand(Popup popup, String brandName) {
        return PopupSummaryResponseDto.builder()
                .id(popup.getId())
                .title(popup.getTitle())
                .summary(popup.getSummary())
                .period(popup.getPeriodText())
                .status(popup.getStatus())
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
                .images(popup.getImages().stream()
                        .map(PopupImageResponseDto::from)
                        .collect(Collectors.toList()))
                .venueName(popup.getVenueName())
                .venueAddress(popup.getVenueAddress())
                .region(popup.getRegion())
                .parkingAvailable(popup.getParkingAvailable())
                .categoryId(popup.getCategory() != null ? popup.getCategory().getId() : null)
                .categoryName(popup.getCategoryName())
                .categorySlug(popup.getCategorySlug())
                .brandId(popup.getBrandId())
                .brandName(brandName)
                .build();
    }
}
