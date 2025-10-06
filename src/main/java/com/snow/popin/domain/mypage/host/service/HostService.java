package com.snow.popin.domain.mypage.host.service;

import com.snow.popin.domain.category.entity.Category;
import com.snow.popin.domain.category.repository.CategoryRepository;
import com.snow.popin.domain.map.entity.Venue;
import com.snow.popin.domain.map.repository.MapRepository;
import com.snow.popin.domain.mypage.host.dto.HostProfileResponseDto;
import com.snow.popin.domain.mypage.host.dto.PopupRegisterRequestDto;
import com.snow.popin.domain.mypage.host.dto.PopupRegisterResponseDto;
import com.snow.popin.domain.mypage.host.dto.VenueRegisterDto;
import com.snow.popin.domain.mypage.host.entity.Host;
import com.snow.popin.domain.mypage.host.repository.HostRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.entity.PopupHours;
import com.snow.popin.domain.popup.entity.Tag;
import com.snow.popin.domain.popup.repository.PopupHoursRepository;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.popup.repository.TagRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 호스트 서비스
 *
 * - 팝업 등록/수정/삭제
 * - 내가 등록한 팝업 목록/상세 조회
 * - 호스트 프로필 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HostService {

    private final HostRepository hostRepository;
    private final PopupRepository popupRepository;
    private final PopupHoursRepository popupHoursRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final MapRepository mapRepository;
    /**
     * 팝업 등록
     *
     * @param user 현재 로그인 사용자
     * @param dto 팝업 등록 요청 DTO
     * @return 생성된 팝업 ID
     */
    @Transactional
    public Long createPopup(User user, PopupRegisterRequestDto dto) {
        Host host = hostRepository.findByUser(user)
                .orElseThrow(() -> {
                    log.error("[HostService] 팝업 등록 실패 - 호스트 권한 없음: userId={}", user.getId());
                    return new GeneralException(ErrorCode.UNAUTHORIZED);
                });

        Popup popup = Popup.create(host.getBrand().getId(), dto);

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));
            popup.setCategory(category);
        }

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            popup.getTags().addAll(tags);
        }

        popupRepository.save(popup);

        if (dto.getHours() != null && !dto.getHours().isEmpty()) {
            List<PopupHours> hours = dto.getHours().stream()
                    .map(hourDto -> PopupHours.create(popup, hourDto))
                    .collect(Collectors.toList());
            popupHoursRepository.saveAll(hours);
        }

        log.info("[HostService] 팝업 등록 완료: popupId={}, userId={}", popup.getId(), user.getId());
        return popup.getId();
    }
    /**
     * 내가 등록한 팝업 목록 조회
     *
     * @param user 현재 로그인 사용자
     * @return 팝업 응답 DTO 리스트
     */
    @Transactional(readOnly = true)
    public Page<PopupRegisterResponseDto> getMyPopups(User user, Pageable pageable) {
        Host host = hostRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.UNAUTHORIZED));

        Page<PopupRegisterResponseDto> result =
                popupRepository.findByBrandId(host.getBrand().getId(), pageable)
                        .map(PopupRegisterResponseDto::fromEntity);

        log.info("[HostService] 내 팝업 목록 조회: userId={}, total={}", user.getId(), result.getTotalElements());
        return result;
    }
    /**
     * 내가 등록한 팝업 상세 조회
     *
     * @param user 현재 로그인 사용자
     * @param id 팝업 ID
     * @return 팝업 응답 DTO
     */
    @Transactional(readOnly = true)
    public PopupRegisterResponseDto getMyPopupDetail(User user, Long id) {
        Host host = hostRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.UNAUTHORIZED));

        Popup popup = popupRepository.findByIdWithTagsAndCategory(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));

        if (!popup.getBrandId().equals(host.getBrand().getId())) {
            log.warn("[HostService] 팝업 상세 조회 권한 없음: userId={}, popupId={}", user.getId(), id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }

        log.info("[HostService] 팝업 상세 조회 완료: userId={}, popupId={}", user.getId(), id);
        return PopupRegisterResponseDto.fromEntity(popup);
    }
    /**
     * 팝업 수정
     *
     * @param user 현재 로그인 사용자
     * @param id 팝업 ID
     * @param dto 팝업 수정 요청 DTO
     */
    @Transactional
    public void updatePopup(User user, Long id, PopupRegisterRequestDto dto) {
        Host host = hostRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.UNAUTHORIZED));

        Popup popup = popupRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));

        if (!popup.getBrandId().equals(host.getBrand().getId())) {
            log.warn("[HostService] 팝업 수정 권한 없음: userId={}, popupId={}", user.getId(), id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }

        popup.update(dto);

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));
            popup.setCategory(category);
        }

        popup.getTags().clear();
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(dto.getTagIds());
            popup.getTags().addAll(tags);
        }

        log.info("[HostService] 팝업 수정 완료: userId={}, popupId={}", user.getId(), id);
    }

    /**
     * 팝업 삭제
     *
     * @param user 현재 로그인 사용자
     * @param id 팝업 ID
     */
    @Transactional
    public void deletePopup(User user, Long id) {
        Host host = hostRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.UNAUTHORIZED));

        Popup popup = popupRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));

        if (!popup.getBrandId().equals(host.getBrand().getId())) {
            log.warn("[HostService] 팝업 삭제 권한 없음: userId={}, popupId={}", user.getId(), id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }

        popupRepository.delete(popup);
        log.info("[HostService] 팝업 삭제 완료: userId={}, popupId={}", user.getId(), id);
    }
    /**
     * 내 호스트 프로필 조회
     *
     * @param user 현재 로그인 사용자
     * @return 호스트 프로필 응답 DTO
     */
    @Transactional(readOnly = true)
    public HostProfileResponseDto getMyHostProfile(User user) {
        Host host = hostRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.UNAUTHORIZED));

        log.info("[HostService] 호스트 프로필 조회 완료: userId={}", user.getId());
        return HostProfileResponseDto.from(host);
    }
    @Transactional
    public void updatePopupVenue(User user, Long popupId, VenueRegisterDto dto) {
        Host host = hostRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.UNAUTHORIZED));

        Popup popup = popupRepository.findById(popupId)
                .orElseThrow(() -> new GeneralException(ErrorCode.NOT_FOUND));

        if (!popup.getBrandId().equals(host.getBrand().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }

        // Venue 생성
        Venue venue = Venue.of(
                dto.getName(),
                dto.getRoadAddress(),
                dto.getJibunAddress(),
                dto.getDetailAddress(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getParkingAvailable()
        );

        venue.setRegionFromAddress();

        mapRepository.save(venue);
        popup.setVenue(venue);

        log.info("[HostService] 팝업 장소 등록 완료: popupId={}, venueId={}, region={}",
                popupId, venue.getId(), venue.getRegion());
    }

}
