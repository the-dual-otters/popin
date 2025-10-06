package com.snow.popin.domain.review.dto;

import com.snow.popin.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 리뷰 리스트 응답 DTO
@Getter
public class ReviewListResponseDto {

    private Long id;
    private Long userId;
    private String userName;
    private String userNickname;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;

    @Builder
    public ReviewListResponseDto(Long id, Long userId, String userName,
                                 String userNickname, String content, Integer rating,
                                 LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userNickname = userNickname;
        this.content = content;
        this.rating = rating;
        this.createdAt = createdAt;
    }

    // Entity에서 간단한 리스트용 DTO로 변환
    public static ReviewListResponseDto from(Review review) {
        return ReviewListResponseDto.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .userName(review.getUser() != null ? review.getUser().getName() : "익명")
                .userNickname(review.getUser() != null ? review.getUser().getNickname() : null)
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .build();
    }
}