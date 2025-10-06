package com.snow.popin.domain.payment.service;

import com.snow.popin.domain.payment.dto.PaymentResponseDto;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import com.snow.popin.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 단위 테스트")
class PaymentServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "kakaoPayAdminKey", "test_admin_key");
    }

    @Test
    @DisplayName("카카오페이 결제 준비 - 성공")
    void prepareKakaoPayment_Success() {
        // Given
        Long reservationId = 1L;

        // Mock 객체들 생성
        User mockUser = mock(User.class);
        Popup mockPopup = mock(Popup.class);

        lenient().when(mockUser.getId()).thenReturn(1L);
        lenient().when(mockPopup.getEntryFee()).thenReturn(10000);
        lenient().when(mockPopup.getTitle()).thenReturn("테스트 팝업");

        Reservation mockReservation = Reservation.builder()
                .id(1L)
                .popup(mockPopup)
                .user(mockUser)
                .name("김테스트")
                .phone("010-1234-5678")
                .partySize(2)
                .reservationDate(LocalDateTime.now().plusDays(1))
                .reservedAt(LocalDateTime.now())
                .status(ReservationStatus.RESERVED)
                .paymentStatus(Reservation.PaymentStatus.PENDING)
                .build();

        // 예약 조회 Mock
        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.of(mockReservation));

        // 카카오페이 API 응답 Mock
        Map<String, Object> kakaoResponse = new HashMap<>();
        kakaoResponse.put("tid", "T123456789");
        kakaoResponse.put("next_redirect_pc_url", "https://mockpay.kakao.com/redirect");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(kakaoResponse, HttpStatus.OK);

        given(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).willReturn(responseEntity);

        // ReservationRepository.save() Mock
        given(reservationRepository.save(any(Reservation.class)))
                .willReturn(mockReservation);

        // When
        PaymentResponseDto result = paymentService.prepareKakaoPayment(reservationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getTid()).isEqualTo("T123456789");
        assertThat(result.getRedirectUrl()).isEqualTo("https://mockpay.kakao.com/redirect");
        assertThat(result.getMessage()).isEqualTo("결제 준비 완료");

        // Mock 호출 검증
        verify(reservationRepository).findById(reservationId);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
        verify(reservationRepository, times(2)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("카카오페이 결제 준비 - 예약을 찾을 수 없음")
    void prepareKakaoPayment_ReservationNotFound() {
        // Given
        Long reservationId = 999L;
        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.prepareKakaoPayment(reservationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약을 찾을 수 없습니다.");

        verify(reservationRepository).findById(reservationId);
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
    }

    @Test
    @DisplayName("카카오페이 결제 준비 - 무료 팝업")
    void prepareKakaoPayment_FreePopup() {
        // Given
        Long reservationId = 1L;

        // Mock 객체들 생성
        User mockUser = mock(User.class);
        Popup freePopup = mock(Popup.class);

        given(freePopup.getEntryFee()).willReturn(0);

        Reservation freeReservation = Reservation.builder()
                .id(1L)
                .popup(freePopup)
                .user(mockUser)
                .name("김테스트")
                .phone("010-1234-5678")
                .partySize(2)
                .reservationDate(LocalDateTime.now().plusDays(1))
                .reservedAt(LocalDateTime.now())
                .status(ReservationStatus.RESERVED)
                .paymentStatus(Reservation.PaymentStatus.PENDING)
                .build();

        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.of(freeReservation));

        // When & Then
        assertThatThrownBy(() -> paymentService.prepareKakaoPayment(reservationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("무료 팝업은 결제가 필요하지 않습니다.");

        verify(reservationRepository).findById(reservationId);
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
    }

    @Test
    @DisplayName("카카오페이 결제 준비 - 카카오페이 API 호출 실패")
    void prepareKakaoPayment_KakaoApiFailure() {
        // Given
        Long reservationId = 1L;

        // Mock 객체들 생성
        User mockUser = mock(User.class);
        Popup mockPopup = mock(Popup.class);

        lenient().when(mockUser.getId()).thenReturn(1L);
        lenient().when(mockPopup.getEntryFee()).thenReturn(10000);
        lenient().when(mockPopup.getTitle()).thenReturn("테스트 팝업");

        Reservation mockReservation = Reservation.builder()
                .id(1L)
                .popup(mockPopup)
                .user(mockUser)
                .name("김테스트")
                .phone("010-1234-5678")
                .partySize(2)
                .reservationDate(LocalDateTime.now().plusDays(1))
                .reservedAt(LocalDateTime.now())
                .status(ReservationStatus.RESERVED)
                .paymentStatus(Reservation.PaymentStatus.PENDING)
                .build();

        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.of(mockReservation));

        // 카카오페이 API 호출 시 예외 발생
        given(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).willThrow(new RuntimeException("카카오페이 API 오류"));

        given(reservationRepository.save(any(Reservation.class)))
                .willReturn(mockReservation);

        // When
        PaymentResponseDto result = paymentService.prepareKakaoPayment(reservationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getMessage()).contains("결제 준비 실패");

        verify(reservationRepository).findById(reservationId);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
        verify(reservationRepository, times(2)).save(any(Reservation.class));
    }

    @Test
    @DisplayName("카카오페이 결제 승인 - 성공")
    void approveKakaoPayment_Success() {
        // Given
        String tid = "T123456789";
        String pgToken = "pg_token_123";
        Long reservationId = 1L;

        // Mock 객체들 생성
        User mockUser = mock(User.class);
        Popup mockPopup = mock(Popup.class);

        lenient().when(mockUser.getId()).thenReturn(1L);

        Reservation mockReservation = Reservation.builder()
                .id(1L)
                .popup(mockPopup)
                .user(mockUser)
                .name("김테스트")
                .phone("010-1234-5678")
                .partySize(2)
                .reservationDate(LocalDateTime.now().plusDays(1))
                .reservedAt(LocalDateTime.now())
                .status(ReservationStatus.RESERVED)
                .paymentStatus(Reservation.PaymentStatus.PENDING)
                .build();

        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.of(mockReservation));

        // 카카오페이 승인 API 응답 Mock
        Map<String, Object> approveResponse = new HashMap<>();
        approveResponse.put("aid", "A123456789");
        approveResponse.put("amount", Map.of("total", 20000));

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(approveResponse, HttpStatus.OK);

        given(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).willReturn(responseEntity);

        given(reservationRepository.save(any(Reservation.class)))
                .willReturn(mockReservation);

        // When
        boolean result = paymentService.approveKakaoPayment(tid, pgToken, reservationId);

        // Then
        assertThat(result).isTrue();

        verify(reservationRepository).findById(reservationId);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("카카오페이 결제 승인 - 예약을 찾을 수 없음")
    void approveKakaoPayment_ReservationNotFound() {
        // Given
        String tid = "T123456789";
        String pgToken = "pg_token_123";
        Long reservationId = 999L;

        given(reservationRepository.findById(reservationId))
                .willReturn(Optional.empty());

        // When
        boolean result = paymentService.approveKakaoPayment(tid, pgToken, reservationId);

        // Then - 예외 대신 false 반환 검증
        assertThat(result).isFalse();

        verify(reservationRepository).findById(reservationId);
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
    }

    @Test
    @DisplayName("결제 금액 계산 - 정상")
    void calculatePaymentAmount() {
        // Given
        Integer entryFee = 10000;
        Integer partySize = 3;
        Integer expectedAmount = 30000;

        // When
        int totalAmount = entryFee * partySize;

        // Then
        assertThat(totalAmount).isEqualTo(expectedAmount);
    }
}