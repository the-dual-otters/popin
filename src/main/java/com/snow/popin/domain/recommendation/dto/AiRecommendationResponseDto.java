package com.snow.popin.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AiRecommendationResponseDto {
    private boolean success;
    private List<Long> recommendedPopupIds;
    private String reasoning; // AI가 추천한 이유
    private String error;

    public static AiRecommendationResponseDto success(List<Long> popupIds, String reasoning) {
        return AiRecommendationResponseDto.builder()
                .success(true)
                .recommendedPopupIds(popupIds)
                .reasoning(reasoning)
                .build();
    }

    public static AiRecommendationResponseDto failure(String error) {
        return AiRecommendationResponseDto.builder()
                .success(false)
                .error(error)
                .build();
    }
}