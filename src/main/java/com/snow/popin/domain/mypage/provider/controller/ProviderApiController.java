package com.snow.popin.domain.mypage.provider.controller;

import com.snow.popin.domain.space.dto.SpaceListResponseDto;
import com.snow.popin.domain.mypage.provider.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ProviderApiController
 * 공간 제공자(Provider) 관련 REST API 컨트롤러.
 * - 마이페이지에서 내가 등록한 공간 목록 조회 기능을 제공한다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/provider")
public class ProviderApiController {

    private final ProviderService providerservice;

    /**
     * 내가 등록한 공간 리스트 조회
     *
     * @return 내가 등록한 공간 리스트
     */
    @GetMapping("/spaces")
    public ResponseEntity<List<SpaceListResponseDto>> loadMySpaceInProfile() {
        log.info("[ProviderApiController] 내 공간 목록 조회 요청");
        List<SpaceListResponseDto> spaces = providerservice.findMySpaces();
        log.info("[ProviderApiController] 내 공간 목록 조회 완료: count={}", spaces.size());
        return ResponseEntity.ok(spaces);
    }
}
