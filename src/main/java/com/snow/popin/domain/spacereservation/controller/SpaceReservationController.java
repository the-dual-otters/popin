package com.snow.popin.domain.spacereservation.controller;

import com.snow.popin.domain.spacereservation.dto.SpaceReservationCreateRequestDto;
import com.snow.popin.domain.spacereservation.dto.SpaceReservationListResponseDto;
import com.snow.popin.domain.spacereservation.dto.SpaceReservationResponseDto;
import com.snow.popin.domain.spacereservation.service.SpaceReservationService;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * SpaceReservationController
 * 공간 예약 관련 API 컨트롤러
 * - 예약 생성, 조회, 승인/거절, 취소, 삭제
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/space-reservations")
@Slf4j
public class SpaceReservationController {

    private final SpaceReservationService reservationService;
    private final UserUtil userUtil;

    /**
     * 예약 요청 생성 (HOST)
     *
     * @param dto 예약 생성 요청 DTO
     * @return 생성된 예약 ID
     */
    @PostMapping
    public ResponseEntity<?> createReservation(@Valid @RequestBody SpaceReservationCreateRequestDto dto) {
        log.info("[SpaceReservationController] 예약 요청 생성");
        Long reservationId = reservationService.createReservation(dto);
        log.info("[SpaceReservationController] 예약 요청 생성 완료: reservationId={}", reservationId);
        return ResponseEntity.ok(Map.of("id", reservationId));
    }

    /**
     * 내가 신청한 예약 목록 조회 (HOST)
     *
     * @return 예약 목록
     */
    @GetMapping("/my-requests")
    public List<SpaceReservationListResponseDto> getMyRequests() {
        log.info("[SpaceReservationController] 내가 신청한 예약 목록 조회 요청");
        List<SpaceReservationListResponseDto> result = reservationService.getMyRequests();
        log.info("[SpaceReservationController] 내가 신청한 예약 목록 조회 완료: count={}", result.size());
        return result;
    }

    /**
     * 내 공간에 신청된 예약 목록 조회 (PROVIDER)
     *
     * @return 예약 목록
     */
    @GetMapping("/my-spaces")
    public List<SpaceReservationListResponseDto> getMySpaceReservations() {
        log.info("[SpaceReservationController] 내 공간 예약 목록 조회 요청");
        List<SpaceReservationListResponseDto> result = reservationService.getMySpaceReservations();
        log.info("[SpaceReservationController] 내 공간 예약 목록 조회 완료: count={}", result.size());
        return result;
    }

    /**
     * 예약 상세 조회
     *
     * @param id 예약 ID
     * @return 예약 상세 응답 DTO
     */
    @GetMapping("/{id}")
    public SpaceReservationResponseDto getReservationDetail(@PathVariable Long id) {
        log.info("[SpaceReservationController] 예약 상세 조회 요청: reservationId={}", id);
        SpaceReservationResponseDto dto = reservationService.getReservationDetail(id);
        log.info("[SpaceReservationController] 예약 상세 조회 완료: reservationId={}", id);
        return dto;
    }

    /**
     * 예약 승인 (PROVIDER)
     *
     * @param id 예약 ID
     * @return 성공 메시지
     */
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptReservation(@PathVariable Long id) {
        log.info("[SpaceReservationController] 예약 승인 요청: reservationId={}", id);
        reservationService.acceptReservation(id);
        log.info("[SpaceReservationController] 예약 승인 완료: reservationId={}", id);
        return ResponseEntity.ok(Map.of("message", "예약이 승인되었습니다."));
    }

    /**
     * 예약 거절 (PROVIDER)
     *
     * @param id 예약 ID
     * @return 성공 메시지
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectReservation(@PathVariable Long id) {
        log.info("[SpaceReservationController] 예약 거절 요청: reservationId={}", id);
        reservationService.rejectReservation(id);
        log.info("[SpaceReservationController] 예약 거절 완료: reservationId={}", id);
        return ResponseEntity.ok(Map.of("message", "예약이 거절되었습니다."));
    }

    /**
     * 예약 취소 (HOST)
     *
     * @param id 예약 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        log.info("[SpaceReservationController] 예약 취소 요청: reservationId={}", id);
        reservationService.cancelReservation(id);
        log.info("[SpaceReservationController] 예약 취소 완료: reservationId={}", id);
        return ResponseEntity.ok(Map.of("message", "예약이 취소되었습니다."));
    }

    /**
     * 예약 삭제 (거절된 예약만, PROVIDER)
     *
     * @param id 예약 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteReservation(@PathVariable Long id) {
        log.info("[SpaceReservationController] 예약 삭제 요청: reservationId={}", id);
        reservationService.deleteReservation(id);
        log.info("[SpaceReservationController] 예약 삭제 완료: reservationId={}", id);
        return ResponseEntity.ok(Map.of("message", "예약이 삭제되었습니다."));
    }
}