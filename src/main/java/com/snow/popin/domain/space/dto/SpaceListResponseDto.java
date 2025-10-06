package com.snow.popin.domain.space.dto;

import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SpaceListResponseDto {
    private Long id;
    private String ownerName;
    private String title;
    private String description;
    private String address;
    private Integer areaSize;
    private boolean mine;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer rentalFee;
    private String coverImageUrl;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    public static SpaceListResponseDto from(Space space, User me) {
        // Venue에서 주소 정보 조합
        String fullAddress = buildFullAddress(space);

        return SpaceListResponseDto.builder()
                .id(space.getId())
                .title(space.getTitle())
                .description(space.getDescription())
                .ownerName(space.getOwner().getName())
                .address(fullAddress)
                .areaSize(space.getAreaSize())
                .startDate(space.getStartDate())
                .endDate(space.getEndDate())
                .rentalFee(space.getRentalFee())
                .coverImageUrl(space.getCoverImageUrl())
                .createdAt(space.getCreatedAt())
                .mine(me != null && java.util.Objects.equals(space.getOwner().getId(), me.getId()))
                .build();
    }

    private static String buildFullAddress(Space space) {
        if (space.getVenue() == null) {
            return "주소 정보 없음";
        }

        StringBuilder addressBuilder = new StringBuilder();

        // 도로명 주소 우선
        if (space.getVenue().getRoadAddress() != null && !space.getVenue().getRoadAddress().trim().isEmpty()) {
            addressBuilder.append(space.getVenue().getRoadAddress());
        }
        // 도로명 주소가 없으면 지번 주소
        else if (space.getVenue().getJibunAddress() != null && !space.getVenue().getJibunAddress().trim().isEmpty()) {
            addressBuilder.append(space.getVenue().getJibunAddress());
        }

        // 상세 주소 추가
        if (space.getVenue().getDetailAddress() != null && !space.getVenue().getDetailAddress().trim().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(" ");
            }
            addressBuilder.append(space.getVenue().getDetailAddress());
        }

        return addressBuilder.length() > 0 ? addressBuilder.toString() : "주소 정보 없음";
    }
}