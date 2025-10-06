package com.snow.popin.domain.reservation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snow.popin.domain.popupReservation.controller.ReservationController;
import com.snow.popin.domain.popupReservation.dto.ReservationRequestDto;
import com.snow.popin.domain.popupReservation.dto.ReservationResponseDto;
import com.snow.popin.domain.popupReservation.dto.TimeSlotDto;
import com.snow.popin.domain.popupReservation.entity.ReservationStatus;
import com.snow.popin.domain.popupReservation.service.ReservationService;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.util.UserUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ReservationController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@DisplayName("예약 컨트롤러 테스트")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private UserUtil userUtil;

    @MockBean(name = "jwtUtil")
    private Object jwtUtil;

    @MockBean(name = "jwtFilter")
    private Object jwtFilter;

    @Test
    @DisplayName("팝업 예약 생성 - 성공")
    void createReservation_Success() throws Exception {
        // given
        Long popupId = 1L;
        Long reservationId = 100L;
        User currentUser = createTestUser();

        ReservationRequestDto requestDto = createReservationRequestDto();

        given(userUtil.getCurrentUser()).willReturn(currentUser);
        given(reservationService.createReservation(eq(currentUser), eq(popupId), any(ReservationRequestDto.class)))
                .willReturn(reservationId);

        // when & then
        mockMvc.perform(post("/api/reservations/popups/{popupId}", popupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.message").value("예약이 완료되었습니다."));
    }

    @Test
    @DisplayName("팝업 예약 생성 - 유효성 검사 실패")
    void createReservation_ValidationFail() throws Exception {
        // given
        Long popupId = 1L;
        ReservationRequestDto invalidDto = new ReservationRequestDto();
        // 필수 필드들이 비어있어 유효성 검사 실패

        // when & then
        mockMvc.perform(post("/api/reservations/popups/{popupId}", popupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("내 예약 목록 조회 - 성공")
    void getMyReservations_Success() throws Exception {
        // given
        User currentUser = createTestUser();
        List<ReservationResponseDto> reservations = Arrays.asList(
                createReservationResponseDto(1L, "팝업 A"),
                createReservationResponseDto(2L, "팝업 B")
        );

        given(userUtil.getCurrentUser()).willReturn(currentUser);
        given(reservationService.getMyReservations(currentUser)).willReturn(reservations);

        // when & then
        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].popupTitle").value("팝업 A"))
                .andExpect(jsonPath("$[1].popupTitle").value("팝업 B"));
    }

    @Test
    @DisplayName("예약 취소 - 성공")
    void cancelReservation_Success() throws Exception {
        // given
        Long reservationId = 100L;
        User currentUser = createTestUser();

        given(userUtil.getCurrentUser()).willReturn(currentUser);
        willDoNothing().given(reservationService).cancelReservation(reservationId, currentUser);

        // when & then
        mockMvc.perform(put("/api/reservations/{reservationId}/cancel", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("예약이 취소되었습니다."));
    }

    @Test
    @DisplayName("방문 완료 처리 - 성공")
    void markAsVisited_Success() throws Exception {
        // given
        Long reservationId = 100L;
        User currentUser = createTestUser();

        given(userUtil.getCurrentUser()).willReturn(currentUser);
        willDoNothing().given(reservationService).markAsVisited(reservationId, currentUser);

        // when & then
        mockMvc.perform(put("/api/reservations/{reservationId}/visit", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("방문 완료로 처리되었습니다."));
    }

    @Test
    @DisplayName("예약 가능한 날짜 목록 조회 - 성공")
    void getAvailableDates_Success() throws Exception {
        // given
        Long popupId = 1L;
        List<LocalDate> availableDates = Arrays.asList(
                LocalDate.of(2024, 12, 20),
                LocalDate.of(2024, 12, 21),
                LocalDate.of(2024, 12, 22)
        );

        given(reservationService.getAvailableDates(popupId)).willReturn(availableDates);

        // when & then
        mockMvc.perform(get("/api/reservations/popups/{popupId}/available-dates", popupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value(availableDates.get(0).toString()))
                .andExpect(jsonPath("$[1]").value(availableDates.get(1).toString()))
                .andExpect(jsonPath("$[2]").value(availableDates.get(2).toString()));
    }

    @Test
    @DisplayName("예약 가능한 시간 슬롯 조회 - 성공")
    void getAvailableTimeSlots_Success() throws Exception {
        // given
        Long popupId = 1L;
        LocalDate date = LocalDate.now().plusDays(1); // 현재 날짜 + 1일로 변경
        List<TimeSlotDto> timeSlots = Arrays.asList(
                TimeSlotDto.createAvailable(LocalTime.of(10, 0), LocalTime.of(11, 0), 5, 10),
                TimeSlotDto.createAvailable(LocalTime.of(11, 0), LocalTime.of(12, 0), 7, 10),
                TimeSlotDto.createUnavailable(LocalTime.of(12, 0), LocalTime.of(13, 0), "점심시간")
        );

        given(reservationService.getAvailableTimeSlots(popupId, date)).willReturn(timeSlots);

        // when & then
        mockMvc.perform(get("/api/reservations/popups/{popupId}/available-slots", popupId)
                        .param("date", date.toString())) // 동적 날짜 사용
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[0].remainingSlots").value(5))
                .andExpect(jsonPath("$[1].available").value(true))
                .andExpect(jsonPath("$[1].remainingSlots").value(3))
                .andExpect(jsonPath("$[2].available").value(false))
                .andExpect(jsonPath("$[2].remainingSlots").value(0));
    }

    private User createTestUser() {
        // User 엔티티에 테스트용 정적 팩토리 메서드가 있다고 가정
        // 실제 User 엔티티의 구조에 맞게 수정 필요
        try {
            // User 클래스에 createForTest 같은 메서드가 있는지 확인 필요
            // 없다면 reflection 사용하거나 mock 객체 사용
            return User.class.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // Mock 객체 사용하는 방법으로 대체
            User user = org.mockito.Mockito.mock(User.class);
            org.mockito.Mockito.when(user.getId()).thenReturn(1L);
            return user;
        }
    }

    private ReservationRequestDto createReservationRequestDto() {
        ReservationRequestDto dto = new ReservationRequestDto();
        dto.setName("홍길동");
        dto.setPhone("010-1234-5678");
        dto.setPartySize(2);
        dto.setReservationDate(LocalDateTime.now().plusDays(1));
        return dto;
    }

    private ReservationResponseDto createReservationResponseDto(Long id, String popupTitle) {
        return ReservationResponseDto.builder()
                .id(id)
                .popupId(1L)
                .popupTitle(popupTitle)
                .popupSummary("테스트 팝업")
                .venueName("테스트 장소")
                .venueAddress("서울시 강남구")
                .name("홍길동")
                .phone("010-1234-5678")
                .partySize(2)
                .reservationDate(LocalDateTime.of(2024, 12, 20, 14, 0))
                .reservedAt(LocalDateTime.of(2024, 12, 19, 10, 0))
                .status(ReservationStatus.RESERVED)
                .statusDescription("예약됨")
                .build();
    }
}