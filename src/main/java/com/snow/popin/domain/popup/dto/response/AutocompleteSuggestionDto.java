package com.snow.popin.domain.popup.dto.response;

import lombok.Getter;

/**
 * 자동완성 제안 응답 DTO
 */
@Getter
public class AutocompleteSuggestionDto {
    private final String text;        // 표시될 텍스트 (예: "브런치 팝업")
    private final String type;        // 타입 ("title" 또는 "tag")
    private final Long popularity;    // 인기도 (조회수 또는 사용 횟수)

    private AutocompleteSuggestionDto(String text, String type, Long popularity) {
        this.text = text;
        this.type = type;
        this.popularity = popularity;
    }

    public static AutocompleteSuggestionDto of(String text, String type, Long popularity) {
        return new AutocompleteSuggestionDto(text, type, popularity);
    }

    public static AutocompleteSuggestionDto fromTitle(String title, Long viewCount) {
        return new AutocompleteSuggestionDto(title, "title", viewCount);
    }

    public static AutocompleteSuggestionDto fromTag(String tagName, Long usageCount) {
        return new AutocompleteSuggestionDto(tagName, "tag", usageCount);
    }
}