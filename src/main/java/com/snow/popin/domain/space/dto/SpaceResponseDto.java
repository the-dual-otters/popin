package com.snow.popin.domain.space.dto;

import com.snow.popin.domain.space.entity.Space;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SpaceResponseDto {
    private Long id;
    private String title;
    private String description;
    private String address;
    private Integer areaSize;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer rentalFee;
    private String contactPhone;
    private String coverImageUrl;
    private Boolean isPublic;
    private Boolean isOfficial;

    private Boolean isHidden;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private OwnerInfo owner;

    @Getter
    @Builder
    public static class OwnerInfo {
        private Long id;
        private String name;
        private String email;
    }

    @Getter
    @Builder
    public static class VenueInfo {
        private String roadAddress;
        private String jibunAddress;
        private String detailAddress;
        private Double latitude;
        private Double longitude;
    }

    private VenueInfo venue;

    public static SpaceResponseDto from(Space space) {
        return SpaceResponseDto.builder()
                .id(space.getId())
                .title(space.getTitle())
                .description(space.getDescription())
                .areaSize(space.getAreaSize())
                .startDate(space.getStartDate())
                .endDate(space.getEndDate())
                .rentalFee(space.getRentalFee())
                .contactPhone(space.getContactPhone())
                .coverImageUrl(space.getCoverImageUrl())
                .isPublic(space.getIsPublic())
                .isOfficial(space.getIsOfficial())
                .isHidden(space.getIsHidden())
                .createdAt(space.getCreatedAt())
                .updatedAt(space.getUpdatedAt())
                .owner(OwnerInfo.builder()
                        .id(space.getOwner().getId())
                        .name(space.getOwner().getName())
                        .email(space.getOwner().getEmail())
                        .build())
                .venue(space.getVenue() != null ? VenueInfo.builder()
                        .roadAddress(space.getVenue().getRoadAddress())
                        .jibunAddress(space.getVenue().getJibunAddress())
                        .detailAddress(space.getVenue().getDetailAddress())
                        .latitude(space.getVenue().getLatitude())
                        .longitude(space.getVenue().getLongitude())
                        .build() : null)
                .build();
    }
}
