package com.snow.popin.domain.mypage.provider.service;

import com.snow.popin.domain.space.dto.SpaceListResponseDto;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.space.repository.SpaceRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ProviderService
 * 공간 제공자 관련 서비스
 * - 마이페이지에서 내가 등록한 공간 목록 조회
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProviderService {

    private final SpaceRepository spaceRepository;
    private final UserUtil userUtil;

    /**
     * 내가 등록한 공간 리스트 조회
     *
     * @return 현재 로그인한 사용자가 등록한 공간 리스트
     */
    @Transactional(readOnly = true)
    public List<SpaceListResponseDto> findMySpaces() {
        User me = userUtil.getCurrentUser();
        log.info("[ProviderService] 내 공간 목록 조회 시작: userId={}, email={}", me.getId(), me.getEmail());

        List<Space> spaces = spaceRepository.findByOwnerAndIsHiddenFalseOrderByCreatedAtDescWithJoins(me);
        log.info("[ProviderService] 내 공간 목록 조회 완료: userId={}, count={}", me.getId(), spaces.size());

        return spaces.stream()
                .map(space -> SpaceListResponseDto.from(space, me))
                .collect(Collectors.toList());
    }
}
