package com.snow.popin.domain.popup.dto.response;

import lombok.Getter;

import java.util.List;

/**
* 자동완성 응답 전체 DTO
 */
@Getter
public class AutocompleteResponseDto {
    private final List<AutocompleteSuggestionDto> suggestions;
    private final String query;
    private final int totalCount;

    private AutocompleteResponseDto(List<AutocompleteSuggestionDto> suggestions, String query) {
        this.suggestions = suggestions;
        this.query = query;
        this.totalCount = suggestions.size();
    }

    public static AutocompleteResponseDto of(List<AutocompleteSuggestionDto> suggestions, String query) {
        return new AutocompleteResponseDto(suggestions, query);
    }

    public static AutocompleteResponseDto empty(String query) {
        return new AutocompleteResponseDto(List.of(), query);
    }
}
