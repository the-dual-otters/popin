package com.snow.popin.domain.recommendation;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.repository.PopupQueryDslRepository;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import com.snow.popin.domain.recommendation.dto.AiRecommendationResponseDto;
import com.snow.popin.domain.recommendation.service.AiRecommendationService;
import com.snow.popin.domain.recommendation.service.GeminiAiService;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI 추천 서비스 테스트")
class AiRecommendationServiceTest {

    @Mock
    private GeminiAiService geminiAiService;

    @Mock
    private PopupRepository popupRepository;

    @Mock
    private PopupQueryDslRepository popupQueryDslRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private AiRecommendationService aiRecommendationService;

    private User testUser;
    private List<Popup> testPopups;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 (Builder 사용, ID는 자동생성이므로 제외)
        testUser = User.builder()
                .email("test@example.com")
                .name("테스트유저")
                .nickname("testUser")
                .password("password123")
                .build();

        // ID는 테스트에서 직접 설정 (리플렉션 사용)
        setFieldValue(testUser, "id", 1L);

        // 테스트 팝업들 (static factory method 사용)
        testPopups = Arrays.asList(
                createPopup(1L, "패션 팝업", PopupStatus.ONGOING),
                createPopup(2L, "게임 팝업", PopupStatus.ONGOING),
                createPopup(3L, "음식 팝업", PopupStatus.ONGOING)
        );
    }

    @Test
    @DisplayName("정상적인 AI 추천 - 성공")
    void getPersonalizedRecommendations_Success() {
        // Given
        Long userId = 1L;
        int limit = 3;
        String aiResponse = "추천 팝업 ID: [1,2,3]\n추천 이유: 사용자 취향에 맞는 팝업들입니다.";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findByUser(testUser)).thenReturn(Collections.emptyList());
        when(popupRepository.findByStatus(PopupStatus.ONGOING)).thenReturn(testPopups);
        when(geminiAiService.generateText(anyString())).thenReturn(aiResponse);

        // When
        AiRecommendationResponseDto result = aiRecommendationService.getPersonalizedRecommendations(userId, limit);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecommendedPopupIds()).hasSize(3);
        assertThat(result.getRecommendedPopupIds()).containsExactly(1L, 2L, 3L);
        assertThat(result.getReasoning()).contains("사용자 취향에 맞는");
    }

    @Test
    @DisplayName("사용자 없음 - 빈 사용자 정보로 처리")
    void getPersonalizedRecommendations_UserNotFound() {
        // Given
        Long userId = 999L;
        int limit = 3;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(popupRepository.findByStatus(PopupStatus.ONGOING)).thenReturn(testPopups);
        when(geminiAiService.generateText(anyString())).thenReturn("추천 팝업 ID: [1,2,3]\n추천 이유: 기본 추천입니다.");
        when(popupQueryDslRepository.findPopularActivePopups(any())).thenReturn(
                new PageImpl<>(testPopups, PageRequest.of(0, limit), testPopups.size())
        );

        // When
        AiRecommendationResponseDto result = aiRecommendationService.getPersonalizedRecommendations(userId, limit);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecommendedPopupIds()).isNotEmpty();
    }

    @Test
    @DisplayName("Gemini API 실패 - 인기 팝업으로 fallback")
    void getPersonalizedRecommendations_GeminiApiFailed() {
        // Given
        Long userId = 1L;
        int limit = 3;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findByUser(testUser)).thenReturn(Collections.emptyList());
        when(popupRepository.findByStatus(PopupStatus.ONGOING)).thenReturn(testPopups);
        when(geminiAiService.generateText(anyString())).thenReturn(null); // API 실패
        when(popupQueryDslRepository.findPopularActivePopups(any())).thenReturn(
                new PageImpl<>(testPopups, PageRequest.of(0, limit), testPopups.size())
        );

        // When
        AiRecommendationResponseDto result = aiRecommendationService.getPersonalizedRecommendations(userId, limit);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReasoning()).contains("현재 인기있는 팝업들을 추천드립니다.");
    }

    @Test
    @DisplayName("잘못된 AI 응답 형식 - 인기 팝업으로 fallback")
    void getPersonalizedRecommendations_InvalidAiResponse() {
        // Given
        Long userId = 1L;
        int limit = 3;
        String invalidResponse = "잘못된 형식의 응답입니다.";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findByUser(testUser)).thenReturn(Collections.emptyList());
        when(popupRepository.findByStatus(PopupStatus.ONGOING)).thenReturn(testPopups);
        when(geminiAiService.generateText(anyString())).thenReturn(invalidResponse);
        when(popupQueryDslRepository.findPopularActivePopups(any())).thenReturn(
                new PageImpl<>(testPopups, PageRequest.of(0, limit), testPopups.size())
        );

        // When
        AiRecommendationResponseDto result = aiRecommendationService.getPersonalizedRecommendations(userId, limit);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReasoning()).contains("현재 인기있는 팝업들을 추천드립니다.");
    }

    @Test
    @DisplayName("빈 팝업 목록 - fallback 처리")
    void getPersonalizedRecommendations_EmptyPopupList() {
        // Given
        Long userId = 1L;
        int limit = 3;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reservationRepository.findByUser(testUser)).thenReturn(Collections.emptyList());
        when(popupRepository.findByStatus(PopupStatus.ONGOING)).thenReturn(Collections.emptyList());
        when(popupQueryDslRepository.findPopularActivePopups(any())).thenReturn(
                new PageImpl<>(Collections.emptyList(), PageRequest.of(0, limit), 0)
        );

        // When
        AiRecommendationResponseDto result = aiRecommendationService.getPersonalizedRecommendations(userId, limit);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRecommendedPopupIds()).isEmpty();
    }

    @Test
    @DisplayName("예외 발생 - fallback 처리")
    void getPersonalizedRecommendations_ExceptionOccurred() {
        // Given
        Long userId = 1L;
        int limit = 3;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(popupRepository.findByStatus(PopupStatus.ONGOING)).thenThrow(new RuntimeException("DB 오류"));
        when(popupQueryDslRepository.findPopularActivePopups(any())).thenReturn(
                new PageImpl<>(testPopups, PageRequest.of(0, limit), testPopups.size())
        );

        // When
        AiRecommendationResponseDto result = aiRecommendationService.getPersonalizedRecommendations(userId, limit);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getReasoning()).contains("현재 인기있는 팝업들을 추천드립니다.");
    }

    private Popup createPopup(Long id, String title, PopupStatus status) {
        // Popup entity의 static factory method 사용
        Popup popup = Popup.createForTest(title, status, null);

        // ID와 brandId 설정 (리플렉션 사용)
        setFieldValue(popup, "id", id);
        setFieldValue(popup, "brandId", 1L);

        return popup;
    }

    // 리플렉션을 사용한 필드 값 설정 헬퍼 메서드
    private void setFieldValue(Object object, String fieldName, Object value) {
        try {
            var field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("필드 설정 실패: " + fieldName, e);
        }
    }
}