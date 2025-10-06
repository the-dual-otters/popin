package com.snow.popin.domain.review.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;

// 리뷰 수정 요청 DTO
@Getter
@NoArgsConstructor
public class ReviewUpdateRequestDto {

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(min = 10, max = 1000, message = "리뷰 내용은 10자 이상 1000자 이하로 작성해주세요.")
    private String content;

    @NotNull(message = "평점은 필수입니다.")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
    private Integer rating;

    @Builder
    public ReviewUpdateRequestDto(String content, Integer rating) {
        this.content = content;
        this.rating = rating;
    }
}