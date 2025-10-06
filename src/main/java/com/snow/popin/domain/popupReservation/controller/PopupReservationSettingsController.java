package com.snow.popin.domain.popupReservation.controller;

import com.snow.popin.domain.popupReservation.dto.PopupCapacitySettingsDto;
import com.snow.popin.domain.popupReservation.service.PopupReservationSettingsService;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reservations/settings")
@RequiredArgsConstructor
@Validated
public class PopupReservationSettingsController {

    private final PopupReservationSettingsService settingsService;
    private final UserUtil userUtil;

    /**
     * 팝업 기본 예약 설정 조회
     */
    @GetMapping("/popups/{popupId}/basic")
    public ResponseEntity<PopupCapacitySettingsDto> getBasicSettings(
            @PathVariable @Positive Long popupId) {
        User currentUser = userUtil.getCurrentUser();
        log.info("[PopupReservationSettingsController] 기본 예약 설정 조회 요청: popupId={}, userId={}", popupId, currentUser.getId());

        PopupCapacitySettingsDto settings = settingsService.getCapacitySettings(popupId, currentUser);

        log.info("[PopupReservationSettingsController] 기본 예약 설정 조회 완료: popupId={}, userId={}", popupId, currentUser.getId());
        return ResponseEntity.ok(settings);
    }
    /**
     * 팝업 기본 예약 설정 수정
     */
    @PutMapping("/popups/{popupId}/basic")
    public ResponseEntity<?> updateBasicSettings(
            @PathVariable @Positive Long popupId,
            @Valid @RequestBody PopupCapacitySettingsDto dto) {

        User currentUser = userUtil.getCurrentUser();
        log.info("[PopupReservationSettingsController] 기본 예약 설정 수정 요청: popupId={}, userId={}", popupId, currentUser.getId());

        settingsService.updateBasicSettings(popupId, dto, currentUser);

        log.info("[PopupReservationSettingsController] 기본 예약 설정 수정 완료: popupId={}, userId={}", popupId, currentUser.getId());
        return ResponseEntity.ok(Map.of(
                "message", "기본 예약 설정이 업데이트되었습니다.",
                "popupId", popupId
        ));
    }
}
