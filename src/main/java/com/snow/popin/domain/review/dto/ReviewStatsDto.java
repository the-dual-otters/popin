package com.snow.popin.domain.review.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

// 리뷰 통계 DTO
@Slf4j
@Getter
public class ReviewStatsDto {

    private Double averageRating;
    private Long totalReviews;

    @Builder
    public ReviewStatsDto(Double averageRating, Long totalReviews) {
        this.averageRating = averageRating != null ? Math.round(averageRating * 10) / 10.0 : 0.0;
        this.totalReviews = totalReviews != null ? totalReviews : 0L;
    }

    public static ReviewStatsDto from(Object[] result) {
        if (result == null || result.length == 0) {
            log.warn("리뷰 통계 결과가 null이거나 비어있습니다.");
            return ReviewStatsDto.builder()
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .build();
        }

        try {
            // 중첩된 배열 처리
            Object[] actualData = null;

            if (result[0] instanceof Object[]) {
                // 중첩된 배열인 경우
                actualData = (Object[]) result[0];
            } else {
                // 일반 배열인 경우
                actualData = result;
            }

            if (actualData == null || actualData.length < 2) {
                return ReviewStatsDto.builder()
                        .averageRating(0.0)
                        .totalReviews(0L)
                        .build();
            }

            Double avgRating = null;
            Long count = null;

            if (actualData[0] != null) {
                if (actualData[0] instanceof Number) {
                    avgRating = ((Number) actualData[0]).doubleValue();
                }
                log.info("평균 평점 파싱: {} -> {}", actualData[0], avgRating);
            }

            if (actualData[1] != null) {
                if (actualData[1] instanceof Number) {
                    count = ((Number) actualData[1]).longValue();
                }
                log.info("리뷰 수 파싱: {} -> {}", actualData[1], count);
            }

            return ReviewStatsDto.builder()
                    .averageRating(avgRating != null ? avgRating : 0.0)
                    .totalReviews(count != null ? count : 0L)
                    .build();

        } catch (Exception e) {
            log.error("리뷰 통계 파싱 중 오류 발생", e);
            return ReviewStatsDto.builder()
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .build();
        }
    }
}