package com.snow.popin.domain.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 사용자 선호도 분석을 위한 DTO
 */
@Getter
@Builder
public class UserPreferenceDto {
    private Long userId;
    private List<String> interests; // 관심 카테고리
    private List<ReservationHistoryDto> reservationHistory; // 예약 이력
    private String location; // 주요 활동 지역
}
