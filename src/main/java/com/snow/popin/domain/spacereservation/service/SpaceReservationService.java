package com.snow.popin.domain.spacereservation.service;

import com.snow.popin.domain.map.entity.Venue;
import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.entity.Host;
import com.snow.popin.domain.mypage.host.repository.HostRepository;
import com.snow.popin.domain.notification.constant.NotificationType;
import com.snow.popin.domain.notification.service.NotificationService;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupStatus;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.space.repository.SpaceRepository;
import com.snow.popin.domain.spacereservation.dto.SpaceReservationCreateRequestDto;
import com.snow.popin.domain.spacereservation.dto.SpaceReservationListResponseDto;
import com.snow.popin.domain.spacereservation.dto.SpaceReservationResponseDto;
import com.snow.popin.domain.spacereservation.entity.SpaceReservation;
import com.snow.popin.domain.spacereservation.entity.ReservationStatus;
import com.snow.popin.domain.spacereservation.repository.SpaceReservationRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SpaceReservationService
 * 공간 예약 관련 비즈니스 로직 처리
 * - 예약 생성, 조회, 승인/거절, 취소, 삭제
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SpaceReservationService {

    private final SpaceReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final PopupRepository popupRepository;
    private final HostRepository hostRepository;
    private final UserUtil userUtil;
    private final NotificationService notificationService;

    /**
     * 공간 예약 생성 (HOST)
     *
     * @param dto 예약 생성 요청 DTO
     * @return 생성된 예약 ID
     */
    @Transactional
    public Long createReservation(SpaceReservationCreateRequestDto dto) {
        User user = userUtil.getCurrentUser();
        log.info("[SpaceReservationService] 예약 생성 요청: userId={}, spaceId={}, popupId={}", user.getId(), dto.getSpaceId(), dto.getPopupId());

        Host hostEntity = hostRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("[SpaceReservationService] 호스트 정보 없음: userId={}", user.getId());
                    return new IllegalArgumentException("호스트 정보가 없습니다.");
                });
        Brand brand = hostEntity.getBrand();

        Space space = spaceRepository.findById(dto.getSpaceId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공간입니다."));
        Popup popup = popupRepository.findById(dto.getPopupId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팝업입니다."));

        SpaceReservation reservation = SpaceReservation.builder()
                .space(space)
                .host(user)
                .popup(popup)
                .brand(brand)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .message(dto.getMessage())
                .contactPhone(dto.getContactPhone())
                .status(ReservationStatus.PENDING)
                .build();

        SpaceReservation saved = reservationRepository.save(reservation);

        notificationService.createNotification(
                space.getOwner().getId(),
                "새로운 공간 예약 신청",
                String.format("%s님이 '%s' 공간에 예약을 신청했습니다.", user.getName(), space.getTitle()),
                NotificationType.RESERVATION,
                "/mypage/provider"
        );

        log.info("[SpaceReservationService] 예약 생성 완료: reservationId={}, spaceId={}, hostId={}", saved.getId(), space.getId(), user.getId());
        return saved.getId();
    }

    /**
     * 내가 신청한 예약 목록 조회 (HOST)
     *
     * @return 예약 목록
     */
    @Transactional(readOnly = true)
    public List<SpaceReservationListResponseDto> getMyRequests() {
        User host = userUtil.getCurrentUser();
        log.info("[SpaceReservationService] 내가 신청한 예약 목록 조회 요청: userId={}", host.getId());

        List<SpaceReservationListResponseDto> result = reservationRepository.findByHostAndIsHiddenFalseOrderByCreatedAtDesc(host)
                .stream()
                .map(SpaceReservationListResponseDto::fromForHost)
                .collect(Collectors.toList());

        log.info("[SpaceReservationService] 내가 신청한 예약 목록 조회 완료: userId={}, count={}", host.getId(), result.size());
        return result;
    }

    /**
     * 내 공간에 신청된 예약 목록 조회 (PROVIDER)
     *
     * @return 예약 목록
     */
    @Transactional(readOnly = true)
    public List<SpaceReservationListResponseDto> getMySpaceReservations() {
        User provider = userUtil.getCurrentUser();
        log.info("[SpaceReservationService] 내 공간 예약 목록 조회 요청: providerId={}", provider.getId());

        List<SpaceReservationListResponseDto> result = reservationRepository.findBySpaceOwnerOrderByCreatedAtDesc(provider)
                .stream()
                .map(SpaceReservationListResponseDto::fromForProvider)
                .collect(Collectors.toList());

        log.info("[SpaceReservationService] 내 공간 예약 목록 조회 완료: providerId={}, count={}", provider.getId(), result.size());
        return result;
    }

    /**
     * 예약 상세 조회
     *
     * @param reservationId 예약 ID
     * @return 예약 상세 응답 DTO
     */
    @Transactional(readOnly = true)
    public SpaceReservationResponseDto getReservationDetail(Long reservationId) {
        User user = userUtil.getCurrentUser();
        log.info("[SpaceReservationService] 예약 상세 조회 요청: reservationId={}, userId={}", reservationId, user.getId());

        SpaceReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.error("[SpaceReservationService] 예약 없음: reservationId={}", reservationId);
                    return new IllegalArgumentException("존재하지 않는 예약입니다.");
                });

        if (!reservation.isOwner(user) && !reservation.isSpaceOwner(user)) {
            log.warn("[SpaceReservationService] 예약 상세 조회 권한 없음: reservationId={}, userId={}", reservationId, user.getId());
            throw new IllegalArgumentException("조회 권한이 없습니다.");
        }

        log.info("[SpaceReservationService] 예약 상세 조회 완료: reservationId={}", reservationId);
        return SpaceReservationResponseDto.from(reservation);
    }

    /**
     * 예약 승인 (PROVIDER)
     *
     * @param reservationId 예약 ID
     */
    @Transactional
    public void acceptReservation(Long reservationId) {
        User currentUser = userUtil.getCurrentUser();
        log.info("[SpaceReservationService] 예약 승인 요청: reservationId={}, providerId={}", reservationId, currentUser.getId());

        SpaceReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        if (!reservation.getSpace().getOwner().equals(currentUser)) {
            log.warn("[SpaceReservationService] 예약 승인 권한 없음: reservationId={}, providerId={}", reservationId, currentUser.getId());
            throw new IllegalArgumentException("해당 공간에 대한 승인 권한이 없습니다.");
        }

        reservation.accept();

        notificationService.createNotification(
                reservation.getHost().getId(),
                "공간 예약 승인",
                String.format("'%s' 공간 예약이 승인되었습니다!", reservation.getSpace().getTitle()),
                NotificationType.RESERVATION,
                "/mypage/host"
        );

        Popup popup = reservation.getPopup();
        if (popup != null && reservation.getSpace() != null) {
            Venue venue = reservation.getSpace().getVenue();
            if (venue != null) {
                popup.setVenue(venue);
            }
        }

        log.info("[SpaceReservationService] 예약 승인 완료: reservationId={}, hostId={}", reservationId, reservation.getHost().getId());
    }

    /**
     * 예약 거절 (PROVIDER)
     *
     * @param reservationId 예약 ID
     */
    @Transactional
    public void rejectReservation(Long reservationId) {
        User provider = userUtil.getCurrentUser();
        log.info("[SpaceReservationService] 예약 거절 요청: reservationId={}, providerId={}", reservationId, provider.getId());

        SpaceReservation reservation = reservationRepository.findByIdAndSpaceOwner(reservationId, provider)
                .orElseThrow(() -> new IllegalArgumentException("예약이 존재하지 않거나 거절 권한이 없습니다."));

        reservation.reject();

        notificationService.createNotification(
                reservation.getHost().getId(),
                "공간 예약 거절",
                String.format("'%s' 공간 예약이 거절되었습니다.", reservation.getSpace().getTitle()),
                NotificationType.RESERVATION,
                "/mypage/host"
        );

        log.info("[SpaceReservationService] 예약 거절 완료: reservationId={}, hostId={}", reservationId, reservation.getHost().getId());
    }

    /**
     * 예약 취소 (HOST)
     *
     * @param reservationId 예약 ID
     */
    @Transactional
    public void cancelReservation(Long reservationId) {
        User host = userUtil.getCurrentUser();
        log.info("[SpaceReservationService] 예약 취소 요청: reservationId={}, hostId={}", reservationId, host.getId());

        SpaceReservation reservation = reservationRepository.findByIdAndHostAndIsHiddenFalse(reservationId, host)
                .orElseThrow(() -> new IllegalArgumentException("예약이 존재하지 않거나 취소 권한이 없습니다."));

        reservation.cancel();

        notificationService.createNotification(
                reservation.getSpace().getOwner().getId(),
                "공간 예약 취소",
                String.format("%s님이 '%s' 공간 예약을 취소했습니다.", host.getName(), reservation.getSpace().getTitle()),
                NotificationType.RESERVATION,
                "/mypage/provider"
        );

        log.info("[SpaceReservationService] 예약 취소 완료: reservationId={}, hostId={}", reservationId, host.getId());
    }

    /**
     * 예약 삭제 (거절된 예약만 가능, PROVIDER)
     *
     * @param reservationId 예약 ID
     */
    @Transactional
    public void deleteReservation(Long reservationId) {
        User provider = userUtil.getCurrentUser();
        log.info("[SpaceReservationService] 예약 삭제 요청: reservationId={}, providerId={}", reservationId, provider.getId());

        SpaceReservation reservation = reservationRepository.findByIdAndSpaceOwner(reservationId, provider)
                .orElseThrow(() -> new IllegalArgumentException("예약이 존재하지 않거나 삭제 권한이 없습니다."));

        if (reservation.getStatus() == ReservationStatus.REJECTED || reservation.getStatus() == ReservationStatus.CANCELLED) {
            reservation.setHidden(true);
            log.info("[SpaceReservationService] 예약 삭제 완료: reservationId={}, providerId={}", reservationId, provider.getId());
        } else {
            log.warn("[SpaceReservationService] 예약 삭제 불가 (승인/진행중): reservationId={}, providerId={}", reservationId, provider.getId());
            throw new IllegalArgumentException("승인되었거나 진행 중인 예약은 삭제(숨김)할 수 없습니다.");
        }
    }
}