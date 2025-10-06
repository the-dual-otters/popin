package com.snow.popin.domain.inquiry.service;

import com.snow.popin.domain.inquiry.dto.InquiryListResponse;
import com.snow.popin.domain.inquiry.entity.TargetType;
import com.snow.popin.domain.inquiry.repository.InquiryRepository;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.popup.repository.PopupRepository;
import com.snow.popin.domain.space.entity.Space;
import com.snow.popin.domain.space.repository.SpaceRepository;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일반 신고 서비스
 * 공통 기능 및 일반 사용자 대상 신고 관련 기능 제공
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InquiryService {

    private final InquiryRepository inquiryRepo;
    private final PopupRepository popupRepo;
    // TODO: 리뷰 만들어지면 연결
    // private final ReviewRepository reviewRepo;
    private final SpaceRepository spaceRepo;
    private final UserRepository userRepo;

    /**
     * 신고 대상의 제목 조회 (공통 메서드)
     */
    public String getTargetTitle(TargetType targetType, Long targetId) {
        try {
            switch (targetType) {
                case POPUP:
                    return popupRepo.findById(targetId)
                            .map(Popup::getTitle)
                            .orElse("삭제된 팝업");
                case REVIEW:
                    return "리뷰 #" + targetId;
                case USER:
                    return userRepo.findById(targetId)
                            .map(User::getEmail)
                            .orElse("유저가 없음");
                case SPACE:
                    return spaceRepo.findById(targetId)
                            .map(Space::getTitle)
                            .orElse("장소 없음");
                case GENERAL:
                    return "일반 문의";
                default:
                    return "알 수 없음";
            }
        } catch (Exception e) {
            return "조회 실패";
        }
    }

}
