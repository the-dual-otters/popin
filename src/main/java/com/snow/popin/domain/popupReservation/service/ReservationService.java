package com.snow.popin.domain.popupReservation.service;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.mypage.host.repository.HostRepository;
import com.snow.popin.domain.payment.service.PaymentService;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupHours;
import com.snow.popin.domain.popup.repository.PopupHoursRepository;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.popupReservation.dto.*;
import com.snow.popin.domain.popupReservation.entity.PopupReservationSettings;
import com.snow.popin.domain.popupReservation.entity.Reservation;
import com.snow.popin.domain.popupReservation.repository.ReservationQueryDslRepository;
import com.snow.popin.domain.popupReservation.repository.ReservationRepository;
import com.snow.popin.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationQueryDslRepository reservationQueryDslRepository;
    private final PopupRepository popupRepository;
    private final HostRepository hostRepository;
    private final BrandRepository brandRepository;
    private final PopupHoursRepository popupHoursRepository;
    private final PopupReservationSettingsService settingsService;

    @Autowired
    private PaymentService paymentService;

    /**
     * 팝업 예약 생성
     */
    @Transactional
    public Long createReservation(User currentUser, Long popupId, ReservationRequestDto dto) {
        log.info("[ReservationService] 예약 생성 요청: popupId={}, userId={}", popupId, currentUser.getId());

        Popup popup = validatePopupForReservation(popupId);

        if (reservationQueryDslRepository.existsActiveReservationByPopupAndUser(popup, currentUser)) {
            log.warn("[ReservationService] 이미 예약된 팝업: popupId={}, userId={}", popupId, currentUser.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 예약한 팝업입니다.");
        }

        PopupReservationSettings settings = settingsService.getSettings(popupId);
        validateReservationRequest(dto, settings);
        validateTimeSlotAvailability(popupId, dto, settings);

        Reservation reservation = Reservation.create(
                popup, currentUser, dto.getName(), dto.getPhone(),
                dto.getPartySize(), dto.getReservationDate()
        );

        Reservation saved = reservationRepository.save(reservation);

        log.info("[ReservationService] 예약 생성 완료: reservationId={}, popupId={}, userId={}, partySize={}",
                saved.getId(), popupId, currentUser.getId(), dto.getPartySize());

        return saved.getId();
    }

    /**
     * 예약 가능한 시간 슬롯 조회
     */
    public List<TimeSlotDto> getAvailableTimeSlots(Long popupId, LocalDate date) {
        log.info("[ReservationService] 예약 가능 슬롯 조회 요청: popupId={}, date={}", popupId, date);

        Popup popup = validatePopupForReservation(popupId);
        PopupReservationSettings settings = settingsService.getSettings(popupId);

        List<PopupHours> operatingHours = popupHoursRepository.findByPopupIdAndDayOfWeek(
                popupId, date.getDayOfWeek().getValue() % 7
        );

        if (operatingHours.isEmpty()) {
            log.info("[ReservationService] 운영 시간 없음: popupId={}, date={}", popupId, date);
            return new ArrayList<>();
        }

        List<TimeSlotDto> timeSlots = new ArrayList<>();
        for (PopupHours hours : operatingHours) {
            timeSlots.addAll(generateTimeSlotsForOperatingHours(
                    popup, settings, date, hours.getOpenTime(), hours.getCloseTime()
            ));
        }

        log.info("[ReservationService] 예약 가능 슬롯 조회 완료: popupId={}, date={}, count={}", popupId, date, timeSlots.size());
        return timeSlots;
    }

    /**
     * 운영 시간 내 시간 슬롯 생성
     */
    private List<TimeSlotDto> generateTimeSlotsForOperatingHours(
            Popup popup, PopupReservationSettings settings,
            LocalDate date, LocalTime startTime, LocalTime endTime) {

        List<TimeSlotDto> slots = new ArrayList<>();
        LocalTime currentTime = startTime;
        int timeInterval = settings.getTimeSlotInterval();
        int maxCapacity = settings.getMaxCapacityPerSlot();

        while (currentTime.isBefore(endTime)) {
            LocalTime slotEndTime = currentTime.plusMinutes(timeInterval);
            if (slotEndTime.isAfter(endTime)) break;

            LocalDateTime slotStart = LocalDateTime.of(date, currentTime);
            LocalDateTime slotEnd = LocalDateTime.of(date, slotEndTime);

            long currentReservations = reservationQueryDslRepository.sumPartySizeByPopupAndReservationDateBetween(
                    popup, slotStart, slotEnd
            );

            TimeSlotDto slot = TimeSlotDto.createAvailable(
                    currentTime, slotEndTime, (int) currentReservations, maxCapacity
            );

            if (!isTimeSlotBookable(slotStart, settings)) {
                slot = TimeSlotDto.createUnavailable(currentTime, slotEndTime, "예약 불가 시간");
            }

            slots.add(slot);
            currentTime = currentTime.plusMinutes(timeInterval);
        }

        return slots;
    }

    /**
     * 시간 슬롯 예약 가능 여부 확인
     */
    private boolean isTimeSlotBookable(LocalDateTime slotStart, PopupReservationSettings settings) {
        LocalDateTime now = LocalDateTime.now();

        if (slotStart.isBefore(now)) return false;
        if (slotStart.toLocalDate().equals(now.toLocalDate()) && !settings.getAllowSameDayBooking()) return false;

        long daysUntilReservation = now.toLocalDate().until(slotStart.toLocalDate()).getDays();
        return daysUntilReservation <= settings.getAdvanceBookingDays();
    }

    /**
     * 예약 취소 (환불 포함)
     */
    @Transactional
    public void cancelReservation(Long reservationId, User currentUser) {
        log.info("[ReservationService] 예약 취소 요청: reservationId={}, userId={}", reservationId, currentUser.getId());

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.error("[ReservationService] 예약 취소 실패 - 예약 없음: reservationId={}", reservationId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "예약이 존재하지 않습니다.");
                });

        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            log.warn("[ReservationService] 예약 취소 권한 없음: reservationId={}, userId={}", reservationId, currentUser.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "예약을 취소할 권한이 없습니다.");
        }

        PopupReservationSettings settings = settingsService.getSettings(reservation.getPopup().getId());
        validateCancellationDeadline(reservation, settings);

        boolean refundProcessed = false;
        if (reservation.isPaymentCompleted()) {
            log.info("[ReservationService] 환불 처리 시작: reservationId={}, amount={}",
                    reservationId, reservation.getPaymentAmount());

            refundProcessed = paymentService.processRefund(reservationId);

            if (!refundProcessed) {
                log.error("[ReservationService] 환불 처리 실패: reservationId={}", reservationId);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "환불 처리 중 오류가 발생했습니다.");
            }

            log.info("[ReservationService] 환불 처리 완료: reservationId={}", reservationId);
        }

        reservation.cancel();
        reservationRepository.save(reservation);

        log.info("[ReservationService] 예약 취소 완료: reservationId={}, userId={}, refunded={}",
                reservationId, currentUser.getId(), refundProcessed);
    }

    /**
     * 환불 가능 여부 체크 (예약 취소 가능 시간 내에서만)
     */
    public boolean isRefundable(Long reservationId, User currentUser) {
        try {
            Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
            if (reservation == null || !reservation.getUser().getId().equals(currentUser.getId())) return false;
            if (!reservation.isPaymentCompleted()) return false;

            PopupReservationSettings settings = settingsService.getSettings(reservation.getPopup().getId());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cancellationDeadline = reservation.getReservationDate()
                    .minusHours(settings.getCancellationDeadlineHours());

            return now.isBefore(cancellationDeadline);

        } catch (Exception e) {
            log.error("[ReservationService] 환불 가능 여부 체크 실패: reservationId={}", reservationId, e);
            return false;
        }
    }

    /**
     * 취소 마감 시간 검증
     */
    private void validateCancellationDeadline(Reservation reservation, PopupReservationSettings settings) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = reservation.getReservationDate().minusHours(settings.getCancellationDeadlineHours());

        if (now.isAfter(deadline)) {
            log.warn("[ReservationService] 취소 불가 - 마감 시간 경과: reservationId={}, deadline={}", reservation.getId(), deadline);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("예약 %d시간 전까지만 취소 가능합니다.", settings.getCancellationDeadlineHours()));
        }
    }

    /**
     * 내 예약 목록 조회
     */
    public List<ReservationResponseDto> getMyReservations(User currentUser) {
        log.info("[ReservationService] 내 예약 목록 조회: userId={}", currentUser.getId());

        List<ReservationResponseDto> list = reservationRepository.findByUser(currentUser)
                .stream()
                .map(ReservationResponseDto::from)
                .collect(Collectors.toList());

        log.info("[ReservationService] 내 예약 목록 조회 완료: userId={}, count={}", currentUser.getId(), list.size());
        return list;
    }

    /**
     * 팝업 예약 현황 조회 (호스트용)
     */
    public List<ReservationResponseDto> getPopupReservations(Long popupId, User currentUser) {
        log.info("[ReservationService] 팝업 예약 현황 조회 요청: popupId={}, userId={}", popupId, currentUser.getId());

        validateHostPermission(popupId, currentUser);

        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> {
                    log.error("[ReservationService] 팝업 예약 현황 조회 실패 - 팝업 없음: popupId={}", popupId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "팝업이 존재하지 않습니다.");
                });

        List<ReservationResponseDto> list = reservationRepository.findByPopup(popup)
                .stream()
                .map(ReservationResponseDto::from)
                .collect(Collectors.toList());

        log.info("[ReservationService] 팝업 예약 현황 조회 완료: popupId={}, count={}", popupId, list.size());
        return list;
    }

    /**
     * 방문 완료 처리
     */
    @Transactional
    public void markAsVisited(Long reservationId, User currentUser) {
        log.info("[ReservationService] 방문 완료 처리 요청: reservationId={}, userId={}", reservationId, currentUser.getId());

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.error("[ReservationService] 방문 완료 처리 실패 - 예약 없음: reservationId={}", reservationId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "예약이 존재하지 않습니다.");
                });

        validateHostPermission(reservation.getPopup().getId(), currentUser);
        reservation.markAsVisited();

        log.info("[ReservationService] 방문 완료 처리 성공: reservationId={}, handledBy={}", reservationId, currentUser.getId());
    }

    /**
     * 예약 가능한 날짜 목록 조회
     */
    public List<LocalDate> getAvailableDates(Long popupId) {
        log.info("[ReservationService] 예약 가능 날짜 조회 요청: popupId={}", popupId);

        Popup popup = validatePopupForReservation(popupId);
        PopupReservationSettings settings = settingsService.getSettings(popupId);

        LocalDate startDate = calculateStartDate(popup, settings);
        LocalDate endDate = calculateEndDate(popup, settings);

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (hasOperatingHours(popupId, current)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }

        log.info("[ReservationService] 예약 가능 날짜 조회 완료: popupId={}, count={}", popupId, dates.size());
        return dates;
    }

    /**
     * 남은 자리를 포함한 예약 가능한 시간 슬롯 조회
     */
    public List<AvailableSlotDto> getAvailableSlots(Long popupId, LocalDate date) {
        log.info("[ReservationService] 예약 가능 슬롯(인원 포함) 조회 요청: popupId={}, date={}", popupId, date);

        Popup popup = validatePopupForReservation(popupId);
        PopupReservationSettings settings = settingsService.getSettings(popupId);

        List<AvailableSlotDto> slots = new ArrayList<>();
        List<PopupHours> hoursList = popupHoursRepository.findByPopupIdAndDayOfWeek(
                popupId, date.getDayOfWeek().getValue() % 7);

        for (PopupHours hours : hoursList) {
            LocalTime current = hours.getOpenTime();
            while (current.plusMinutes(settings.getTimeSlotInterval()).isBefore(hours.getCloseTime())
                    || current.plusMinutes(settings.getTimeSlotInterval()).equals(hours.getCloseTime())) {

                LocalTime slotStart = current;
                LocalTime slotEnd = current.plusMinutes(settings.getTimeSlotInterval());

                long reservedCount = reservationQueryDslRepository.sumPartySizeByPopupAndReservationDateBetween(
                        popup, date.atTime(slotStart), date.atTime(slotEnd));

                int remaining = settings.getMaxCapacityPerSlot() - (int) reservedCount;
                slots.add(AvailableSlotDto.of(slotStart, slotEnd, Math.max(remaining, 0)));

                current = slotEnd;
            }
        }

        log.info("[ReservationService] 예약 가능 슬롯(인원 포함) 조회 완료: popupId={}, date={}, count={}", popupId, date, slots.size());
        return slots;
    }

    // ========== 유틸리티 메서드들 ==========

    private Popup validatePopupForReservation(Long popupId) {
        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> {
                    log.error("[ReservationService] 팝업 조회 실패: popupId={}", popupId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "팝업이 존재하지 않습니다.");
                });

        if (!popup.getReservationAvailable()) {
            log.warn("[ReservationService] 예약 불가 팝업: popupId={}", popupId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 팝업은 예약을 받을 수 없습니다.");
        }

        return popup;
    }

    private void validateReservationRequest(ReservationRequestDto dto, PopupReservationSettings settings) {
        if (dto.getReservationDate() == null || dto.getReservationDate().isBefore(LocalDateTime.now())) {
            log.warn("[ReservationService] 유효하지 않은 예약 일시: {}", dto.getReservationDate());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약 일시가 올바르지 않습니다.");
        }

        if (!settings.isValidPartySize(dto.getPartySize())) {
            log.warn("[ReservationService] 유효하지 않은 예약 인원: partySize={}, maxPartySize={}", dto.getPartySize(), settings.getMaxPartySize());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("예약 인원은 1명 이상 %d명 이하여야 합니다.", settings.getMaxPartySize()));
        }
    }

    private void validateTimeSlotAvailability(Long popupId, ReservationRequestDto dto, PopupReservationSettings settings) {
        LocalDate date = dto.getReservationDate().toLocalDate();
        LocalTime time = dto.getReservationDate().toLocalTime();

        List<TimeSlotDto> availableSlots = getAvailableTimeSlots(popupId, date);

        TimeSlotDto matchedSlot = availableSlots.stream()
                .filter(slot -> slot.getStartTime().equals(time))
                .findFirst()
                .orElse(null);

        if (matchedSlot == null || !matchedSlot.isAvailable()) {
            log.warn("[ReservationService] 예약 불가 슬롯 선택: popupId={}, date={}, time={}", popupId, date, time);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 시간은 예약이 불가능합니다.");
        }

        if (!matchedSlot.canAccommodate(dto.getPartySize())) {
            log.warn("[ReservationService] 예약 인원 초과: popupId={}, date={}, time={}, 요청인원={}, 잔여={}",
                    popupId, date, time, dto.getPartySize(), matchedSlot.getRemainingSlots());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("해당 시간대에는 %d명만 예약 가능합니다.", matchedSlot.getRemainingSlots()));
        }
    }

    private void validateHostPermission(Long popupId, User currentUser) {
        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> {
                    log.error("[ReservationService] 팝업 조회 실패 (권한 확인): popupId={}", popupId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "팝업이 존재하지 않습니다.");
                });

        Brand brand = brandRepository.findById(popup.getBrandId())
                .orElseThrow(() -> {
                    log.error("[ReservationService] 브랜드 조회 실패: brandId={}", popup.getBrandId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "브랜드가 존재하지 않습니다.");
                });

        boolean isMember = hostRepository.existsByBrandAndUser(brand, currentUser.getId());
        if (!isMember) {
            log.warn("[ReservationService] 호스트 권한 없음: popupId={}, userId={}", popupId, currentUser.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }

    private LocalDate calculateStartDate(Popup popup, PopupReservationSettings settings) {
        LocalDate popupStart = popup.getStartDate() != null ? popup.getStartDate() : LocalDate.now();
        LocalDate today = LocalDate.now();
        LocalDate settingsStart = settings.getAllowSameDayBooking() ? today : today.plusDays(1);

        return popupStart.isAfter(settingsStart) ? popupStart : settingsStart;
    }

    private LocalDate calculateEndDate(Popup popup, PopupReservationSettings settings) {
        LocalDate popupEnd = popup.getEndDate() != null ? popup.getEndDate() : LocalDate.now().plusMonths(3);
        LocalDate maxAdvanceDate = LocalDate.now().plusDays(settings.getAdvanceBookingDays());

        return popupEnd.isBefore(maxAdvanceDate) ? popupEnd : maxAdvanceDate;
    }

    private boolean hasOperatingHours(Long popupId, LocalDate date) {
        return !popupHoursRepository.findByPopupIdAndDayOfWeek(
                popupId, date.getDayOfWeek().getValue() % 7
        ).isEmpty();
    }
}
