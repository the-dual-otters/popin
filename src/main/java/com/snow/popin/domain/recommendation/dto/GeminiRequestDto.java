package com.snow.popin.domain.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Gemini API 요청 DTO
 */
@Getter
@Builder
public class GeminiRequestDto {

    @JsonProperty("contents")
    private List<Content> contents;

    @JsonProperty("generationConfig")
    private GenerationConfig generationConfig;

    @Getter
    @Builder
    public static class Content {
        @JsonProperty("parts")
        private List<Part> parts;

        @JsonProperty("role")
        private String role;
    }

    @Getter
    @Builder
    public static class Part {
        @JsonProperty("text")
        private String text;
    }

    @Getter
    @Builder
    public static class GenerationConfig {
        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("topK")
        private Integer topK;

        @JsonProperty("topP")
        private Double topP;

        @JsonProperty("maxOutputTokens")
        private Integer maxOutputTokens;
    }
}