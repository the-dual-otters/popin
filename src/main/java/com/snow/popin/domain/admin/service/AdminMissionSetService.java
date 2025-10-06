package com.snow.popin.domain.admin.service;

import com.snow.popin.domain.mission.dto.request.MissionSetCreateRequestDto;
import com.snow.popin.domain.mission.dto.request.MissionSetUpdateRequestDto;
import com.snow.popin.domain.mission.dto.response.MissionSetAdminDto;
import com.snow.popin.domain.mission.entity.MissionSet;
import com.snow.popin.domain.mission.constant.MissionSetStatus;
import com.snow.popin.domain.mission.repository.MissionRepository;
import com.snow.popin.domain.mission.repository.MissionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMissionSetService {

    private final MissionSetRepository missionSetRepository;
    private final MissionRepository missionRepository;
    private final QrCodeService qrCodeService;

    /**
     * 목록 조회
     * @param pageable
     * @param popupId
     * @param status
     * @return
     */
    public Page<MissionSetAdminDto> getMissionSets(Pageable pageable, Long popupId, MissionSetStatus status) {
        Page<MissionSet> sets;
        if (popupId != null) {
            sets = missionSetRepository.findByPopupId(popupId, pageable);
        } else if (status != null) {
            sets = missionSetRepository.findByStatus(status, pageable);
        } else {
            sets = missionSetRepository.findAll(pageable);
        }
        return sets.map(MissionSetAdminDto::from);
    }

    /**
     * 상세 조회
     * @param id
     * @return
     */
    public MissionSetAdminDto getMissionSetDetail(UUID id) {
        MissionSet set = missionSetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MissionSet not found"));
        return MissionSetAdminDto.from(set);
    }

    /**
     * 미션셋 생성
     * @param req
     * @return
     */
    @Transactional
    public MissionSetAdminDto createMissionSet(MissionSetCreateRequestDto req) {
        MissionSet set = MissionSet.builder()
                .popupId(req.getPopupId())
                .requiredCount(req.getRequiredCount())
                .status(req.getStatus())
                .rewardPin(req.getRewardPin())
                .build();

        missionSetRepository.save(set); // INSERT 발생

        try {
            String qrUrl = qrCodeService.generateMissionSetQr(set.getId());
            set.setQrImageUrl(qrUrl); // 영속 상태라 더티체킹으로 UPDATE 됨
            log.info("[AdminMissionService] QR 생성 성공 - setId={}, qrUrl={}", set.getId(), qrUrl);
        } catch (Exception ex) {
            log.error("[AdminMissionService] QR 생성 실패 - setId={}, msg={}", set.getId(), ex.getMessage(), ex);
        }

        log.info("[AdminMissionService] 미션셋 생성 완료 - setId={}, popupId={}", set.getId(), set.getPopupId());
        return MissionSetAdminDto.from(set);
    }

    /**
     * 미션 셋 삭제
     * @param id
     */
    public void deleteMissionSet(UUID id) {
        missionSetRepository.deleteById(id);
    }


    /**
     * 미션 셋 수정
     * @param id
     * @param request
     * @return
     */
    @Transactional
    public MissionSetAdminDto updateMissionSet(UUID id, MissionSetUpdateRequestDto request) {
        MissionSet set = missionSetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("MissionSet not found: " + id));

        if (request.getRequiredCount() != null) {
            set.setRequiredCount(request.getRequiredCount());
        }
        if (request.getStatus() != null) {
            set.setStatus(request.getStatus());
        }
        if (request.getRewardPin() != null) {
            set.setRewardPin(request.getRewardPin());
        }

        return MissionSetAdminDto.from(set);
    }

}
