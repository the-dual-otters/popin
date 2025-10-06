package com.snow.popin.domain.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snow.popin.domain.recommendation.dto.GeminiRequestDto;
import com.snow.popin.domain.recommendation.dto.GeminiResponseDto;
import com.snow.popin.global.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Service
public class GeminiAiService {

    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // RestTemplate Bean이 없는 경우 생성자에서 직접 생성
    public GeminiAiService(GeminiProperties geminiProperties, ObjectMapper objectMapper) {
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(geminiProperties.getTimeout()))
                .setReadTimeout(Duration.ofMillis(geminiProperties.getTimeout()))
                .build();
    }

    /**
     * Gemini API를 호출하여 텍스트 생성
     */
    public String generateText(String prompt) {
        try {
            // 요청 DTO 생성
            GeminiRequestDto request = createRequest(prompt);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiProperties.getApi().getKey());

            // 요청 엔티티 생성
            HttpEntity<GeminiRequestDto> entity = new HttpEntity<>(request, headers);

            log.info("Gemini API 호출 시작 - prompt length: {}", prompt.length());

            // API 호출
            ResponseEntity<GeminiResponseDto> response = restTemplate.exchange(
                    geminiProperties.getApi().getUrl(),
                    HttpMethod.POST,
                    entity,
                    GeminiResponseDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            } else {
                log.error("Gemini API 호출 실패 - status: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생", e);
            return null;
        }
    }

    /**
     * 요청 DTO 생성
     */
    private GeminiRequestDto createRequest(String prompt) {
        GeminiRequestDto.Part part = GeminiRequestDto.Part.builder()
                .text(prompt)
                .build();

        GeminiRequestDto.Content content = GeminiRequestDto.Content.builder()
                .parts(Arrays.asList(part))
                .role("user")
                .build();

        GeminiRequestDto.GenerationConfig config = GeminiRequestDto.GenerationConfig.builder()
                .temperature(0.7)
                .topK(40)
                .topP(0.95)
                .maxOutputTokens(1024)
                .build();

        return GeminiRequestDto.builder()
                .contents(Arrays.asList(content))
                .generationConfig(config)
                .build();
    }

    /**
     * 응답에서 텍스트 추출
     */
    private String extractTextFromResponse(GeminiResponseDto response) {
        if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            GeminiResponseDto.Candidate candidate = response.getCandidates().get(0);
            if (candidate.getContent() != null &&
                    candidate.getContent().getParts() != null &&
                    !candidate.getContent().getParts().isEmpty()) {

                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }
}
