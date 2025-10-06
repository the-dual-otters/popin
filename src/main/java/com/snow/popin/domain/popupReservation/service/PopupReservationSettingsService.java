package com.snow.popin.domain.popupReservation.service;

import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.mypage.host.repository.HostRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.popupReservation.dto.PopupCapacitySettingsDto;
import com.snow.popin.domain.popupReservation.entity.PopupReservationSettings;
import com.snow.popin.domain.popupReservation.repository.PopupReservationSettingsRepository;
import com.snow.popin.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopupReservationSettingsService {

    private final PopupReservationSettingsRepository settingsRepository;
    private final PopupRepository popupRepository;
    private final BrandRepository brandRepository;
    private final HostRepository hostRepository;

    /**
     * 팝업 예약 설정 조회
     * 설정이 없으면 기본값으로 자동 생성
     */
    @Cacheable(value = "popupReservationSettings", key = "#popupId")
    public PopupReservationSettings getSettings(Long popupId) {
        log.info("[PopupReservationSettingsService] 예약 설정 조회 요청: popupId={}", popupId);

        PopupReservationSettings settings = settingsRepository.findByPopupId(popupId)
                .orElseGet(() -> createDefaultSettings(popupId));

        PopupReservationSettings applied = applyDefaults(settings);
        log.info("[PopupReservationSettingsService] 예약 설정 조회 완료: popupId={}, interval={}, maxCapacity={}",
                popupId, applied.getTimeSlotInterval(), applied.getMaxCapacityPerSlot());
        return applied;
    }

    /**
     * null 값 보정 (기본값 적용)
     */
    private PopupReservationSettings applyDefaults(PopupReservationSettings settings) {
        if (settings.getTimeSlotInterval() == null) {
            settings.setTimeSlotInterval(30);
        }
        if (settings.getMaxCapacityPerSlot() == null) {
            settings.setMaxCapacityPerSlot(10);
        }
        if (settings.getMaxPartySize() == null) {
            settings.setMaxPartySize(5);
        }
        if (settings.getAdvanceBookingDays() == null) {
            settings.setAdvanceBookingDays(30);
        }
        if (settings.getAllowSameDayBooking() == null) {
            settings.setAllowSameDayBooking(true);
        }
        return settings;
    }

    /**
     * 예약 설정 조회 (DTO 형태, 권한 체크 포함)
     */
    public PopupCapacitySettingsDto getCapacitySettings(Long popupId, User currentUser) {
        log.info("[PopupReservationSettingsService] 기본 예약 설정 조회 요청: popupId={}, userId={}", popupId, currentUser.getId());

        validateHostPermission(popupId, currentUser);
        PopupReservationSettings settings = getSettings(popupId);

        log.info("[PopupReservationSettingsService] 기본 예약 설정 조회 완료: popupId={}, userId={}", popupId, currentUser.getId());
        return PopupCapacitySettingsDto.from(settings);
    }

    /**
     * 기본 예약 설정 업데이트
     */
    @Transactional
    @CacheEvict(value = "popupReservationSettings", key = "#popupId")
    public void updateBasicSettings(Long popupId, PopupCapacitySettingsDto dto, User currentUser) {
        log.info("[PopupReservationSettingsService] 기본 예약 설정 수정 요청: popupId={}, userId={}, maxCapacity={}, interval={}",
                popupId, currentUser.getId(), dto.getMaxCapacityPerSlot(), dto.getTimeSlotInterval());

        validateHostPermission(popupId, currentUser);

        PopupReservationSettings settings = getSettings(popupId);
        settings.updateBasicSettings(dto.getMaxCapacityPerSlot(), dto.getTimeSlotInterval());

        settingsRepository.save(settings);
        log.info("[PopupReservationSettingsService] 기본 예약 설정 수정 완료: popupId={}, userId={}", popupId, currentUser.getId());
    }

    /**
     * 기본 설정으로 팝업 예약 설정 생성
     */
    @Transactional
    public PopupReservationSettings createDefaultSettings(Long popupId) {
        log.info("[PopupReservationSettingsService] 기본 예약 설정 생성 요청: popupId={}", popupId);

        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "팝업이 존재하지 않습니다."));

        PopupReservationSettings settings = PopupReservationSettings.createDefault(popup);
        applyDefaults(settings);

        PopupReservationSettings saved = settingsRepository.save(settings);
        log.info("[PopupReservationSettingsService] 기본 예약 설정 생성 완료: popupId={}", popupId);
        return saved;
    }

    /**
     * 호스트 권한 검증
     */
    private void validateHostPermission(Long popupId, User currentUser) {
        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "팝업이 존재하지 않습니다."));

        Brand brand = brandRepository.findById(popup.getBrandId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "브랜드가 존재하지 않습니다."));

        boolean isMember = hostRepository.existsByBrandAndUser(brand, currentUser.getId());
        if (!isMember) {
            log.warn("[PopupReservationSettingsService] 권한 없음: popupId={}, userId={}", popupId, currentUser.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "예약 설정을 변경할 권한이 없습니다.");
        }
    }
}
