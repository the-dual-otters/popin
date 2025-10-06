package com.snow.popin.domain.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snow.popin.domain.recommendation.dto.GeminiResponseDto;
import com.snow.popin.domain.recommendation.service.GeminiAiService;
import com.snow.popin.global.config.GeminiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Gemini AI 서비스 테스트")
class GeminiAiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GeminiProperties geminiProperties;

    @Mock
    private GeminiProperties.Api apiProperties;

    private ObjectMapper objectMapper;
    private GeminiAiService geminiAiService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // GeminiProperties 기본 설정
        when(geminiProperties.getApi()).thenReturn(apiProperties);
        when(geminiProperties.getTimeout()).thenReturn(30000);
        when(apiProperties.getKey()).thenReturn("test-api-key");
        when(apiProperties.getUrl()).thenReturn("https://test-api.com/generate");

        geminiAiService = new GeminiAiService(geminiProperties, objectMapper);

        // RestTemplate 주입 (리플렉션 사용)
        try {
            var field = GeminiAiService.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            field.set(geminiAiService, restTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("정상적인 AI 텍스트 생성 - 성공")
    void generateText_Success() {
        // Given
        String prompt = "팝업 추천해주세요";
        String expectedText = "추천 팝업: [1, 2, 3]";

        GeminiResponseDto response = createSuccessResponse(expectedText);
        ResponseEntity<GeminiResponseDto> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://test-api.com/generate"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GeminiResponseDto.class)
        )).thenReturn(responseEntity);

        // When
        String result = geminiAiService.generateText(prompt);

        // Then
        assertThat(result).isEqualTo(expectedText);
    }

    @Test
    @DisplayName("API 호출 실패 - null 반환")
    void generateText_ApiCallFailed_ReturnNull() {
        // Given
        String prompt = "팝업 추천해주세요";

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GeminiResponseDto.class)
        )).thenThrow(new RestClientException("API 호출 실패"));

        // When
        String result = geminiAiService.generateText(prompt);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("HTTP 오류 응답 - null 반환")
    void generateText_HttpError_ReturnNull() {
        // Given
        String prompt = "팝업 추천해주세요";

        ResponseEntity<GeminiResponseDto> errorResponse = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GeminiResponseDto.class)
        )).thenReturn(errorResponse);

        // When
        String result = geminiAiService.generateText(prompt);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("빈 응답 본문 - null 반환")
    void generateText_EmptyBody_ReturnNull() {
        // Given
        String prompt = "팝업 추천해주세요";

        ResponseEntity<GeminiResponseDto> emptyResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GeminiResponseDto.class)
        )).thenReturn(emptyResponse);

        // When
        String result = geminiAiService.generateText(prompt);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("후보자가 없는 응답 - null 반환")
    void generateText_NoCandidates_ReturnNull() {
        // Given
        String prompt = "팝업 추천해주세요";

        GeminiResponseDto response = createEmptyResponse();
        ResponseEntity<GeminiResponseDto> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GeminiResponseDto.class)
        )).thenReturn(responseEntity);

        // When
        String result = geminiAiService.generateText(prompt);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("내용이 없는 응답 - null 반환")
    void generateText_NoContent_ReturnNull() {
        // Given
        String prompt = "팝업 추천해주세요";

        GeminiResponseDto response = createResponseWithNoContent();
        ResponseEntity<GeminiResponseDto> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GeminiResponseDto.class)
        )).thenReturn(responseEntity);

        // When
        String result = geminiAiService.generateText(prompt);

        // Then
        assertThat(result).isNull();
    }

    // 헬퍼 메서드들
    private GeminiResponseDto createSuccessResponse(String text) {
        try {
            GeminiResponseDto response = new GeminiResponseDto();

            GeminiResponseDto.Part part = new GeminiResponseDto.Part();
            setPrivateField(part, "text", text);

            GeminiResponseDto.Content content = new GeminiResponseDto.Content();
            setPrivateField(content, "parts", Arrays.asList(part));

            GeminiResponseDto.Candidate candidate = new GeminiResponseDto.Candidate();
            setPrivateField(candidate, "content", content);

            setPrivateField(response, "candidates", Arrays.asList(candidate));

            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GeminiResponseDto createEmptyResponse() {
        try {
            GeminiResponseDto response = new GeminiResponseDto();
            setPrivateField(response, "candidates", Collections.emptyList());
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GeminiResponseDto createResponseWithNoContent() {
        try {
            GeminiResponseDto response = new GeminiResponseDto();

            GeminiResponseDto.Candidate candidate = new GeminiResponseDto.Candidate();
            setPrivateField(candidate, "content", null);

            setPrivateField(response, "candidates", Arrays.asList(candidate));

            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        var field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}