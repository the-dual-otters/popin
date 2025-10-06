package com.snow.popin.domain.mypage.host.controller;

import com.snow.popin.domain.mypage.host.dto.HostProfileResponseDto;
import com.snow.popin.domain.mypage.host.dto.PopupRegisterRequestDto;
import com.snow.popin.domain.mypage.host.dto.PopupRegisterResponseDto;
import com.snow.popin.domain.mypage.host.dto.VenueRegisterDto;
import com.snow.popin.domain.mypage.host.service.HostService;
import com.snow.popin.domain.space.service.FileStorageService;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 호스트 관련 REST 컨트롤러
 *
 * - 팝업 등록/조회/수정/삭제
 * - 호스트 프로필 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/hosts")
@RequiredArgsConstructor
public class HostController {

    private final HostService hostService;
    private final UserUtil userUtil;
    private final FileStorageService fileStorageService;

    /**
     * 팝업 등록
     *
     * @param dto 팝업 등록 요청 DTO
     * @return 팝업 ID 및 성공 메시지
     */
    @PostMapping("/popups")
    public ResponseEntity<?> createPopup(@RequestBody PopupRegisterRequestDto dto) {
        User currentUser = userUtil.getCurrentUser();
        log.info("[HostController] 팝업 등록 요청: userId={}, title={}", currentUser.getId(), dto.getTitle());
        Long id = hostService.createPopup(currentUser, dto);
        log.info("[HostController] 팝업 등록 완료: popupId={}, userId={}", id, currentUser.getId());
        return ResponseEntity.ok(Map.of("id", id, "message", "팝업 등록 완료"));
    }
    /**
     * 내가 등록한 팝업 목록 조회
     *
     * @return 팝업 응답 DTO 리스트
     */
    @GetMapping("/popups")
    public ResponseEntity<Page<PopupRegisterResponseDto>> getMyPopups(Pageable pageable) {
        User currentUser = userUtil.getCurrentUser();
        log.info("[HostController] 내 팝업 목록 조회 요청: userId={}, page={}", currentUser.getId(), pageable.getPageNumber());
        Page<PopupRegisterResponseDto> result = hostService.getMyPopups(currentUser, pageable);
        log.info("[HostController] 내 팝업 목록 조회 완료: userId={}, count={}", currentUser.getId(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }
    /**
     * 내가 등록한 팝업 상세 조회
     *
     * @param id 팝업 ID
     * @return 팝업 응답 DTO
     */
    @GetMapping("/popups/{id}")
    public ResponseEntity<PopupRegisterResponseDto> getMyPopupDetail(@PathVariable Long id) {
        User currentUser = userUtil.getCurrentUser();
        log.info("[HostController] 팝업 상세 조회 요청: userId={}, popupId={}", currentUser.getId(), id);
        PopupRegisterResponseDto dto = hostService.getMyPopupDetail(currentUser, id);
        log.info("[HostController] 팝업 상세 조회 완료: userId={}, popupId={}", currentUser.getId(), id);
        return ResponseEntity.ok(dto);
    }
    /**
     * 팝업 수정
     *
     * @param id 팝업 ID
     * @param dto 팝업 수정 요청 DTO
     * @return 성공 메시지
     */
    @PutMapping("/popups/{id}")
    public ResponseEntity<?> updatePopup(@PathVariable Long id, @RequestBody PopupRegisterRequestDto dto) {
        User user = userUtil.getCurrentUser();
        log.info("[HostController] 팝업 수정 요청: userId={}, popupId={}", user.getId(), id);
        hostService.updatePopup(user, id, dto);
        log.info("[HostController] 팝업 수정 완료: userId={}, popupId={}", user.getId(), id);
        return ResponseEntity.ok(Map.of("message", "팝업 수정 완료"));
    }
    /**
     * 팝업 삭제
     *
     * @param id 팝업 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/popups/{id}")
    public ResponseEntity<?> deletePopup(@PathVariable Long id) {
        User user = userUtil.getCurrentUser();
        log.info("[HostController] 팝업 삭제 요청: userId={}, popupId={}", user.getId(), id);
        hostService.deletePopup(user, id);
        log.info("[HostController] 팝업 삭제 완료: userId={}, popupId={}", user.getId(), id);
        return ResponseEntity.ok(Map.of("message", "팝업 삭제 완료"));
    }
    /**
     * 내 호스트 프로필 조회
     *
     * @return 호스트 프로필 응답 DTO
     */
    @GetMapping("/me")
    public ResponseEntity<HostProfileResponseDto> getMyHostProfile() {
        User user = userUtil.getCurrentUser();
        log.info("[HostController] 호스트 프로필 조회 요청: userId={}", user.getId());
        HostProfileResponseDto profile = hostService.getMyHostProfile(user);
        log.info("[HostController] 호스트 프로필 조회 완료: userId={}", user.getId());
        return ResponseEntity.ok(profile);
    }
    /**
     * 팝업 장소 등록/변경
     *
     * @param id 팝업 ID
     * @param dto 장소 정보 DTO
     * @return 성공 메시지
     */
    @PostMapping("/popups/{id}/venue")
    public ResponseEntity<?> updatePopupVenue(@PathVariable Long id,
                                              @RequestBody VenueRegisterDto dto) {
        User user = userUtil.getCurrentUser();
        hostService.updatePopupVenue(user, id, dto);
        return ResponseEntity.ok(Map.of("message", "장소가 등록되었습니다"));
    }

    @PostMapping("/upload/image")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        log.info("[HostController] 이미지 업로드 요청: filename={}", file.getOriginalFilename());
        try {
            String imageUrl = fileStorageService.save(file);
            log.info("[HostController] 이미지 업로드 성공: url={}", imageUrl);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (Exception e) {
            log.error("[HostController] 이미지 업로드 실패: error={}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
