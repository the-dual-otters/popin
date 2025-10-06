package com.snow.popin.domain.review.dto;

import com.snow.popin.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 리뷰 응답 DTO
@Getter
public class ReviewResponseDto {

    private Long id;
    private Long popupId;
    private String popupTitle;
    private Long userId;
    private String userName;
    private String userNickname;
    private String content;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isBlocked;

    @Builder
    public ReviewResponseDto(Long id, Long popupId, String popupTitle, Long userId,
                             String userName, String userNickname, String content,
                             Integer rating, LocalDateTime createdAt, LocalDateTime updatedAt,
                             Boolean isBlocked) {
        this.id = id;
        this.popupId = popupId;
        this.popupTitle = popupTitle;
        this.userId = userId;
        this.userName = userName;
        this.userNickname = userNickname;
        this.content = content;
        this.rating = rating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isBlocked = isBlocked;
    }

    // Entity에서 DTO로 변환
    public static ReviewResponseDto from(Review review) {
        return ReviewResponseDto.builder()
                .id(review.getId())
                .popupId(review.getPopupId())
                .popupTitle(review.getPopup() != null ? review.getPopup().getTitle() : null)
                .userId(review.getUserId())
                .userName(review.getUser() != null ? review.getUser().getName() : null)
                .userNickname(review.getUser() != null ? review.getUser().getNickname() : null)
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .isBlocked(review.getIsBlocked())
                .build();
    }
}