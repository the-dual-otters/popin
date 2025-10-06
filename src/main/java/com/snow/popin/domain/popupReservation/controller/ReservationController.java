package com.snow.popin.domain.popupReservation.controller;

import com.snow.popin.domain.popupReservation.dto.AvailableSlotDto;
import com.snow.popin.domain.popupReservation.dto.ReservationRequestDto;
import com.snow.popin.domain.popupReservation.dto.ReservationResponseDto;
import com.snow.popin.domain.popupReservation.dto.TimeSlotDto;
import com.snow.popin.domain.popupReservation.service.ReservationService;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 예약 관련 REST 컨트롤러
 *
 * - 팝업 예약 생성
 * - 내 예약 목록 조회
 * - 예약 취소
 * - 팝업별 예약 현황 조회 (호스트용)
 * - 방문 완료 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
public class ReservationController {

    private final ReservationService reservationService;
    private final UserUtil userUtil;

    /**
     * 팝업 예약 생성
     *
     * @param popupId 예약할 팝업 ID
     * @param dto 예약 요청 DTO
     */
    @PostMapping("/popups/{popupId}")
    public ResponseEntity<?> createReservation(
            @PathVariable @Positive Long popupId,
            @Valid @RequestBody ReservationRequestDto dto) {

        User currentUser = userUtil.getCurrentUser();
        log.info("[ReservationController] 예약 생성 요청: popupId={}, userId={}", popupId, currentUser.getId());

        Long reservationId = reservationService.createReservation(currentUser, popupId, dto);

        log.info("[ReservationController] 예약 생성 완료: reservationId={}, popupId={}, userId={}", reservationId, popupId, currentUser.getId());
        return ResponseEntity.ok(Map.of(
                "reservationId", reservationId,
                "message", "예약이 완료되었습니다.",
                "popupId", popupId
        ));
    }

    /**
     * 현재 로그인 사용자의 예약 목록 조회
     *
     * @return 예약 응답 DTO 리스트
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponseDto>> getMyReservations() {
        User currentUser = userUtil.getCurrentUser();
        log.info("[ReservationController] 내 예약 목록 조회 요청: userId={}", currentUser.getId());

        List<ReservationResponseDto> reservations = reservationService.getMyReservations(currentUser);

        log.info("[ReservationController] 내 예약 목록 조회 완료: userId={}, count={}", currentUser.getId(), reservations.size());
        return ResponseEntity.ok(reservations);
    }

    /**
     * 예약 취소
     *
     * @param reservationId 취소할 예약 ID
     */
    @PutMapping("/{reservationId}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable @Positive Long reservationId) {
        User currentUser = userUtil.getCurrentUser();
        log.info("[ReservationController] 예약 취소 요청: reservationId={}, userId={}", reservationId, currentUser.getId());

        reservationService.cancelReservation(reservationId, currentUser);

        log.info("[ReservationController] 예약 취소 완료: reservationId={}, userId={}", reservationId, currentUser.getId());
        return ResponseEntity.ok(Map.of(
                "message", "예약이 취소되었습니다.",
                "reservationId", reservationId
        ));
    }

    /**
     * 특정 팝업에 대한 예약 현황 조회 (호스트 권한 필요)
     *
     * @param popupId 팝업 ID
     * @return 예약 응답 DTO 리스트
     */
    @GetMapping("/popups/{popupId}")
    public ResponseEntity<List<ReservationResponseDto>> getPopupReservations(
            @PathVariable @Positive Long popupId) {
        User currentUser = userUtil.getCurrentUser();
        log.info("[ReservationController] 팝업 예약 현황 조회 요청: popupId={}, userId={}", popupId, currentUser.getId());

        List<ReservationResponseDto> reservations = reservationService.getPopupReservations(popupId, currentUser);

        log.info("[ReservationController] 팝업 예약 현황 조회 완료: popupId={}, count={}", popupId, reservations.size());
        return ResponseEntity.ok(reservations);
    }

    /**
     * 예약을 방문 완료로 처리
     *
     * @param reservationId 예약 ID
     * @return 방문 완료 메시지
     */
    @PutMapping("/{reservationId}/visit")
    public ResponseEntity<?> markAsVisited(@PathVariable @Positive Long reservationId) {
        User currentUser = userUtil.getCurrentUser();
        log.info("[ReservationController] 예약 방문 완료 처리 요청: reservationId={}, userId={}", reservationId, currentUser.getId());

        reservationService.markAsVisited(reservationId, currentUser);

        log.info("[ReservationController] 예약 방문 완료 처리 성공: reservationId={}, userId={}", reservationId, currentUser.getId());
        return ResponseEntity.ok(Map.of(
                "message", "방문 완료로 처리되었습니다.",
                "reservationId", reservationId
        ));
    }

    /**
     * 특정 팝업의 예약 가능한 날짜 목록 조회
     */
    @GetMapping("/popups/{popupId}/available-dates")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @PathVariable @Positive Long popupId) {
        log.info("[ReservationController] 예약 가능 날짜 조회 요청: popupId={}", popupId);

        List<LocalDate> availableDates = reservationService.getAvailableDates(popupId);

        log.info("[ReservationController] 예약 가능 날짜 조회 완료: popupId={}, count={}", popupId, availableDates.size());
        return ResponseEntity.ok(availableDates);
    }

    /**
     * 특정 팝업의 특정 날짜에 예약 가능한 시간 슬롯 조회
     */
    @GetMapping("/popups/{popupId}/available-slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableTimeSlots(
            @PathVariable @Positive Long popupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @FutureOrPresent LocalDate date) {

        log.info("[ReservationController] 예약 가능 시간 슬롯 조회 요청: popupId={}, date={}", popupId, date);

        List<TimeSlotDto> availableSlots = reservationService.getAvailableTimeSlots(popupId, date);

        log.info("[ReservationController] 예약 가능 시간 슬롯 조회 완료: popupId={}, date={}, count={}", popupId, date, availableSlots.size());
        return ResponseEntity.ok(availableSlots);
    }

    /**
     * 특정 날짜의 예약 슬롯 예약 인원 수 조회
     */
    @GetMapping("/popups/{popupId}/available-slots/with-capacity")
    public ResponseEntity<List<AvailableSlotDto>> getAvailableSlots(
            @PathVariable Long popupId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("[ReservationController] 예약 가능 슬롯(인원 포함) 조회 요청: popupId={}, date={}", popupId, date);

        List<AvailableSlotDto> slots = reservationService.getAvailableSlots(popupId, date);

        log.info("[ReservationController] 예약 가능 슬롯(인원 포함) 조회 완료: popupId={}, date={}, count={}", popupId, date, slots.size());
        return ResponseEntity.ok(slots);
    }
}
