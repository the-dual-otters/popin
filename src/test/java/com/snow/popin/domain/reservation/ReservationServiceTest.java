package com.snow.popin.domain.reservation;

import com.snow.popin.domain.mypage.host.dto.PopupHourResponseDto;
import com.snow.popin.domain.mypage.host.dto.PopupRegisterRequestDto;
import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.mypage.host.repository.HostRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupHours;
import com.snow.popin.domain.popup.repository.PopupHoursRepository;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.popupReservation.dto.ReservationRequestDto;
import com.snow.popin.domain.popupReservation.dto.ReservationResponseDto;
import com.snow.popin.domain.popupReservation.dto.TimeSlotDto;
import com.snow.popin.domain.popupReservation.entity.PopupReservationSettings;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.repository.ReservationQueryDslRepository;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import com.snow.popin.domain.popupReservation.service.PopupReservationSettingsService;
import com.snow.popin.domain.popupReservation.service.ReservationService;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationQueryDslRepository reservationQueryDslRepository;

    @Mock
    private PopupRepository popupRepository;

    @Mock
    private HostRepository hostRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private PopupHoursRepository popupHoursRepository;

    @Mock
    private PopupReservationSettingsService settingsService;

    @Test
    @DisplayName("예약 생성 성공")
    void createReservation_Success() {
        // given
        User user = createTestUser(1L);
        Popup popup = createTestPopup(true);
        PopupReservationSettings settings = createTestSettings(popup);

        ReservationRequestDto dto = new ReservationRequestDto();
        dto.setReservationDate(LocalDate.now().plusDays(1).atTime(14, 0));
        dto.setName(user.getName());
        dto.setPhone("010-1234-5678");
        dto.setPartySize(2);

        when(popupRepository.findById(anyLong())).thenReturn(Optional.of(popup));
        when(settingsService.getSettings(anyLong())).thenReturn(settings);
        when(reservationQueryDslRepository.existsActiveReservationByPopupAndUser(any(Popup.class), any(User.class))).thenReturn(false);
        when(popupHoursRepository.findByPopupIdAndDayOfWeek(anyLong(), any(Integer.class))).thenReturn(List.of(createTestPopupHours(popup)));
        when(reservationQueryDslRepository.sumPartySizeByPopupAndReservationDateBetween(any(Popup.class), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer((Answer<Reservation>) invocation -> {
            Reservation reservation = invocation.getArgument(0);
            ReflectionTestUtils.setField(reservation, "id", 1L);
            return reservation;
        });

        // when
        Long reservationId = reservationService.createReservation(user, 1L, dto);

        // then
        assertThat(reservationId).isNotNull();
        assertThat(reservationId).isEqualTo(1L);
    }

    @Test
    @DisplayName("팝업을 찾을 수 없을 때 예약 생성 실패")
    void createReservation_PopupNotFound() {
        // given
        User user = createTestUser(1L);
        ReservationRequestDto dto = new ReservationRequestDto();

        when(popupRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResponseStatusException.class, () -> {
            reservationService.createReservation(user, 1L, dto);
        });
    }

    @Test
    @DisplayName("예약이 불가능할 때 예약 생성 실패")
    void createReservation_NotAvailable() {
        // given
        User user = createTestUser(1L);
        Popup popup = createTestPopup(false);
        ReservationRequestDto dto = new ReservationRequestDto();

        when(popupRepository.findById(anyLong())).thenReturn(Optional.of(popup));

        // when & then
        assertThrows(ResponseStatusException.class, () -> {
            reservationService.createReservation(user, 1L, dto);
        });
    }

    @Test
    @DisplayName("이미 예약이 존재할 때 예약 생성 실패")
    void createReservation_AlreadyExists() {
        // given
        User user = createTestUser(1L);
        Popup popup = createTestPopup(true);
        ReservationRequestDto dto = new ReservationRequestDto();

        when(popupRepository.findById(anyLong())).thenReturn(Optional.of(popup));
        when(reservationQueryDslRepository.existsActiveReservationByPopupAndUser(any(Popup.class), any(User.class))).thenReturn(true);

        // when & then
        assertThrows(ResponseStatusException.class, () -> {
            reservationService.createReservation(user, 1L, dto);
        });
    }

    @Test
    @DisplayName("예약 가능한 시간 슬롯이 아닐 때 예약 생성 실패")
    void createReservation_TimeSlotNotAvailable() {
        // given
        User user = createTestUser(1L);
        Popup popup = createTestPopup(true);
        PopupReservationSettings settings = createTestSettings(popup);

        ReservationRequestDto dto = new ReservationRequestDto();
        dto.setReservationDate(LocalDateTime.now().plusDays(1).withHour(9)); // 운영시간 외
        dto.setName(user.getName());
        dto.setPhone("010-1234-5678");
        dto.setPartySize(2);

        when(popupRepository.findById(anyLong())).thenReturn(Optional.of(popup));
        when(settingsService.getSettings(anyLong())).thenReturn(settings);
        when(reservationQueryDslRepository.existsActiveReservationByPopupAndUser(any(Popup.class), any(User.class))).thenReturn(false);
        when(popupHoursRepository.findByPopupIdAndDayOfWeek(anyLong(), any(Integer.class))).thenReturn(List.of(createTestPopupHours(popup)));

        // when & then
        assertThrows(ResponseStatusException.class, () -> {
            reservationService.createReservation(user, 1L, dto);
        });
    }

    @Test
    @DisplayName("팝업 예약 현황 조회 성공")
    void getPopupReservations_Success() {
        // given
        User user = createTestUser(1L);
        Popup popup = createTestPopup(true);
        Brand brand = createTestBrand();

        when(popupRepository.findById(anyLong())).thenReturn(Optional.of(popup));
        when(brandRepository.findById(anyLong())).thenReturn(Optional.of(brand));
        when(hostRepository.existsByBrandAndUser(any(Brand.class), anyLong())).thenReturn(true);
        when(reservationRepository.findByPopup(any(Popup.class))).thenReturn(Collections.singletonList(createTestReservation(user, popup)));

        // when
        List<ReservationResponseDto> reservations = reservationService.getPopupReservations(1L, user);

        // then
        assertThat(reservations).hasSize(1);
    }

    @Test
    @DisplayName("내 예약 목록 조회 성공")
    void getMyReservations_Success() {
        // given
        User user = createTestUser(1L);
        Popup popup = createTestPopup(true);
        when(reservationRepository.findByUser(any(User.class))).thenReturn(Collections.singletonList(createTestReservation(user, popup)));

        // when
        List<ReservationResponseDto> reservations = reservationService.getMyReservations(user);

        // then
        assertThat(reservations).hasSize(1);
    }

    @Test
    @DisplayName("예약 취소 성공")
    void cancelReservation_Success() {
        // given
        User user = createTestUser(1L);
        Popup popup = createTestPopup(true);

        Reservation reservation = Reservation.create(popup, user, user.getName(), "010-1234-5678", 2, LocalDateTime.now().plusDays(2));
        ReflectionTestUtils.setField(reservation, "id", 1L);

        PopupReservationSettings settings = createTestSettings(popup);

        when(reservationRepository.findById(anyLong())).thenReturn(Optional.of(reservation));
        when(settingsService.getSettings(anyLong())).thenReturn(settings);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        // when
        reservationService.cancelReservation(1L, user);

        // then
        // No exception thrown
    }

    @Test
    @DisplayName("방문 완료 처리 성공")
    void markAsVisited_Success() {
        // given
        User user = createTestUser(1L);
        Popup popup = createTestPopup(true);
        Reservation reservation = createTestReservation(user, popup);
        Brand brand = createTestBrand();

        when(reservationRepository.findById(anyLong())).thenReturn(Optional.of(reservation));
        when(popupRepository.findById(anyLong())).thenReturn(Optional.of(popup));
        when(brandRepository.findById(anyLong())).thenReturn(Optional.of(brand));
        when(hostRepository.existsByBrandAndUser(any(Brand.class), anyLong())).thenReturn(true);

        // when
        reservationService.markAsVisited(1L, user);

        // then
        // No exception thrown
    }

    @Test
    @DisplayName("예약 가능 날짜 조회 성공")
    void getAvailableDates_Success() {
        // given
        Popup popup = createTestPopup(true);
        PopupReservationSettings settings = createTestSettings(popup);

        when(popupRepository.findById(anyLong())).thenReturn(Optional.of(popup));
        when(settingsService.getSettings(anyLong())).thenReturn(settings);
        when(popupHoursRepository.findByPopupIdAndDayOfWeek(anyLong(), any(Integer.class))).thenReturn(List.of(createTestPopupHours(popup)));

        // when
        List<LocalDate> availableDates = reservationService.getAvailableDates(1L);

        // then
        assertThat(availableDates).isNotEmpty();
    }

    @Test
    @DisplayName("예약 가능 시간 슬롯 조회 성공")
    void getAvailableTimeSlots_Success() {
        // given
        Popup popup = createTestPopup(true);
        PopupReservationSettings settings = createTestSettings(popup);

        when(popupRepository.findById(anyLong())).thenReturn(Optional.of(popup));
        when(settingsService.getSettings(anyLong())).thenReturn(settings);
        when(popupHoursRepository.findByPopupIdAndDayOfWeek(anyLong(), any(Integer.class))).thenReturn(List.of(createTestPopupHours(popup)));
        when(reservationQueryDslRepository.sumPartySizeByPopupAndReservationDateBetween(any(Popup.class), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        // when
        List<TimeSlotDto> availableSlots = reservationService.getAvailableTimeSlots(1L, LocalDate.now().plusDays(1));

        // then
        assertThat(availableSlots).isNotEmpty();
    }

    // Helper methods
    private User createTestUser(Long id) {
        User user = User.builder()
                .email("test@test.com")
                .password("password")
                .name("testUser")
                .nickname("testNick")
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Popup createTestPopup(boolean reservationAvailable) {
        PopupRegisterRequestDto dto = new PopupRegisterRequestDto();
        dto.setTitle("Test Popup");
        dto.setReservationAvailable(reservationAvailable);
        Popup popup = Popup.create(1L, dto);
        ReflectionTestUtils.setField(popup, "id", 1L);
        ReflectionTestUtils.setField(popup, "brandId", 1L);
        ReflectionTestUtils.setField(popup, "startDate", LocalDate.now());
        ReflectionTestUtils.setField(popup, "endDate", LocalDate.now().plusDays(30));
        return popup;
    }

    private Brand createTestBrand() {
        Brand brand = Brand.builder()
                .name("Test Brand")
                .build();
        ReflectionTestUtils.setField(brand, "id", 1L);
        return brand;
    }

    private Reservation createTestReservation(User user, Popup popup) {
        Reservation reservation = Reservation.create(popup, user, user.getName(), "010-1234-5678", 2, LocalDateTime.now().plusDays(2));
        ReflectionTestUtils.setField(reservation, "id", 1L);
        return reservation;
    }

    private PopupHours createTestPopupHours(Popup popup) {
        PopupHourResponseDto dto = new PopupHourResponseDto();
        dto.setDayOfWeek(LocalDate.now().plusDays(1).getDayOfWeek().getValue() % 7);
        dto.setOpenTime("10:00");
        dto.setCloseTime("22:00");
        PopupHours hours = PopupHours.create(popup, dto);
        ReflectionTestUtils.setField(hours, "openTime", LocalTime.of(10, 0));
        ReflectionTestUtils.setField(hours, "closeTime", LocalTime.of(22, 0));
        return hours;
    }

    private PopupReservationSettings createTestSettings(Popup popup) {
        PopupReservationSettings settings = PopupReservationSettings.builder()
                .popup(popup)
                .maxCapacityPerSlot(10)
                .timeSlotInterval(30)
                .maxPartySize(6)
                .allowSameDayBooking(true)
                .advanceBookingDays(30)
                .cancellationDeadlineHours(24)
                .build();
        ReflectionTestUtils.setField(settings, "popupId", 1L);
        return settings;
    }
}