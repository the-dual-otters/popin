package com.snow.popin.domain.space.controller;

import com.snow.popin.domain.space.dto.SpaceCreateRequestDto;
import com.snow.popin.domain.space.dto.SpaceListResponseDto;
import com.snow.popin.domain.space.dto.SpaceResponseDto;
import com.snow.popin.domain.space.dto.SpaceUpdateRequestDto;
import com.snow.popin.domain.space.service.SpaceService;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;

/**
 * SpaceController
 * 공간 등록, 조회, 검색, 수정, 삭제 및 신고 API를 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces")
@Slf4j
public class SpaceController {

    private final SpaceService spaceService;
    private final UserUtil userUtil;

    /**
     * 모든 공간 목록 조회
     *
     * @return 모든 공간 리스트
     */
    @GetMapping
    public List<SpaceListResponseDto> listAllSpaces(Pageable pageable) {
        User me = userUtil.getCurrentUser();
        log.info("[SpaceController] 전체 공간 목록 조회 요청: userId={}", me.getId());
        List<SpaceListResponseDto> result = spaceService.listAll(me, pageable);
        log.info("[SpaceController] 전체 공간 목록 조회 완료: count={}", result.size());
        return result;
    }

    /**
     * 공간 검색
     *
     * @param keyword  제목/설명 검색 키워드 (선택)
     * @param location 주소 검색 키워드 (선택)
     * @param minArea  최소 면적 (선택)
     * @param maxArea  최대 면적 (선택)
     * @return 검색 조건에 맞는 공간 리스트
     */
    @GetMapping("/search")
    public List<SpaceListResponseDto> searchSpaces(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer minArea,
            @RequestParam(required = false) Integer maxArea
    ) {
        User me = userUtil.getCurrentUser();
        log.info("[SpaceController] 공간 검색 요청: userId={}, keyword={}, location={}, minArea={}, maxArea={}",
                me.getId(), keyword, location, minArea, maxArea);

        List<SpaceListResponseDto> result = spaceService.searchSpaces(me, keyword, location, minArea, maxArea);
        log.info("[SpaceController] 공간 검색 완료: userId={}, count={}", me.getId(), result.size());
        return result;
    }

    /**
     * 내가 등록한 공간 목록 조회
     *
     * @return 내 공간 리스트
     */
    @GetMapping("/mine")
    public List<SpaceListResponseDto> listMySpaces() {
        User me = userUtil.getCurrentUser();
        log.info("[SpaceController] 내 공간 목록 조회 요청: userId={}", me.getId());
        List<SpaceListResponseDto> result = spaceService.listMine(me);
        log.info("[SpaceController] 내 공간 목록 조회 완료: userId={}, count={}", me.getId(), result.size());
        return result;
    }

    /**
     * 공간 상세 조회
     *
     * @param id 공간 ID
     * @return 공간 상세 응답 DTO
     */
    @GetMapping("/{id}")
    public SpaceResponseDto getDetail(@PathVariable Long id) {
        User me = null;
        if (userUtil.isAuthenticated()) {
            me = userUtil.getCurrentUser();
        }
        log.info("[SpaceController] 공간 상세 조회 요청: userId={}, spaceId={}", me != null ? me.getId() : null, id);
        SpaceResponseDto dto = spaceService.getDetail(me, id);
        log.info("[SpaceController] 공간 상세 조회 완료: spaceId={}", id);
        return dto;
    }

    /**
     * 공간 등록
     *
     * @param dto 공간 등록 요청 DTO
     * @param br  바인딩 결과 (유효성 검증)
     * @return 생성된 공간 ID
     */
    @PostMapping
    public ResponseEntity<?> create(@Valid @ModelAttribute SpaceCreateRequestDto dto,
                                    BindingResult br) {
        if (br.hasErrors()) {
            log.warn("[SpaceController] 공간 등록 유효성 실패: errors={}", br.getAllErrors());
            return badRequest(br);
        }
        User me = userUtil.getCurrentUser();
        log.info("[SpaceController] 공간 등록 요청: userId={}, title={}", me.getId(), dto.getTitle());
        Long id = spaceService.create(me, dto);
        log.info("[SpaceController] 공간 등록 완료: userId={}, spaceId={}", me.getId(), id);
        return ResponseEntity.ok(Map.of("id", id));
    }

    /**
     * 공간 게시글 수정
     *
     * @param id  공간 ID
     * @param dto 공간 수정 요청 DTO
     * @param br  바인딩 결과
     * @return 성공 여부 응답
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @Valid @ModelAttribute SpaceUpdateRequestDto dto,
                                    BindingResult br) {
        if (br.hasErrors()) {
            log.warn("[SpaceController] 공간 수정 유효성 실패: spaceId={}, errors={}", id, br.getAllErrors());
            return badRequest(br);
        }
        User me = userUtil.getCurrentUser();
        log.info("[SpaceController] 공간 수정 요청: userId={}, spaceId={}", me.getId(), id);
        spaceService.update(me, id, dto);
        log.info("[SpaceController] 공간 수정 완료: userId={}, spaceId={}", me.getId(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * 공간 게시글 삭제
     *
     * @param id 공간 ID
     * @return 성공 여부 응답
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        User me = userUtil.getCurrentUser();
        log.info("[SpaceController] 공간 삭제 요청: userId={}, spaceId={}", me.getId(), id);
        spaceService.deleteSpace(me, id);
        log.info("[SpaceController] 공간 삭제 완료: userId={}, spaceId={}", me.getId(), id);
        return ResponseEntity.ok().build();
    }

    /**
     * 공간 문의 등록
     *
     * @param id 공간 ID
     * @return 202 Accepted 응답
     */
    @PostMapping("/{id}/inquiries")
    public ResponseEntity<?> createInquiry(@PathVariable Long id) {
        log.info("[SpaceController] 공간 문의 등록 요청: spaceId={}", id);
        return ResponseEntity.accepted().build();
    }

    /**
     * 공간 신고
     *
     * @param id 공간 ID
     * @return 신고 처리 메시지
     */
    @PostMapping("/{id}/reports")
    public ResponseEntity<?> report(@PathVariable Long id) {
        User me = userUtil.getCurrentUser();
        log.info("[SpaceController] 공간 신고 요청: userId={}, spaceId={}", me.getId(), id);
        spaceService.hideSpace(me, id);
        log.info("[SpaceController] 공간 신고 완료: userId={}, spaceId={}", me.getId(), id);
        return ResponseEntity.ok(Map.of("message", "신고가 접수되어 해당 공간이 숨겨졌습니다."));
    }

    /**
     * 유효성 검증 실패 시 에러 응답 생성
     *
     * @param br 바인딩 결과
     * @return 에러 응답 (400 Bad Request)
     */
    private ResponseEntity<Map<String, Object>> badRequest(BindingResult br) {
        Map<String, String> errors = new HashMap<>();
        br.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }
}
