package com.snow.popin.domain.admin.service;

import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.constant.UserStatus;
import com.snow.popin.domain.user.dto.UserDetailResponse;
import com.snow.popin.domain.user.dto.UserSearchResponse;
import com.snow.popin.domain.user.dto.UserStatusUpdateResponse;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepo;

    /**
     * 회원 검색
     */
    public Page<UserSearchResponse> searchUser(
            String searchType, String keyword, Role role, Pageable pageable) {
        Specification<User> spec = createSearchSpecification(searchType, keyword, role);
        Page<User> users = userRepo.findAll(spec, pageable);

        return users.map(UserSearchResponse::from);
    }

    /**
     * 회원 상세 정보 조회 by email
     */
    public UserDetailResponse getUserDetailByEmail(String email){
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        return UserDetailResponse.from(user);
    }

    /**
     * 회원 상세 정보 조회 by id
     */
    public UserDetailResponse getUserDetailById(Long id){
        User user = userRepo.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        return UserDetailResponse.from(user);
    }

    /**
     * 사용자 상태 변경
     */
    @Transactional
    public UserStatusUpdateResponse updateUserStatus(Long userId, UserStatus status){
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        // 관리자 계정은 비활성화 불가
        if (user.getRole().name().equals("ADMIN")){
            throw new GeneralException(ErrorCode.BAD_REQUEST, "관리자 계정은 비활성화 할 수 없습니다.");
        }

        user.updateStatus(status);
        User savedUser = userRepo.save(user);

        return UserStatusUpdateResponse.of(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getStatus()
        );
    }

    /**
     * 전체 회원 수 조회
     */
    public Long getTotalUserCount(){
        return userRepo.count();
    }

    /**
     * 역할별 회원 수 조회
     */
    public Map<String, Long> getUserCountByRole(){
        Map<String, Long> roleState = new HashMap<>();

        // 각 역할별로 회원 수 조회
        for (Role role : Role.values()){
            Long count = userRepo.countByRole(role);
            roleState.put(role.name(), count);
        }

        return roleState;
    }

    private Specification<User> createSearchSpecification(String searchType, String keyword, Role role) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 키워드 검색
            if (StringUtils.hasText(keyword)) {
                String likeKeyword = "%" + keyword.toLowerCase() + "%";

                switch (searchType != null ? searchType.toLowerCase() : "name") {
                    case "email":
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("email")), likeKeyword));
                        break;
                    case "nickname":
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("nickname")), likeKeyword));
                        break;
                    case "phone":
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("phone")), likeKeyword));
                        break;
                    case "name":
                    default:
                        predicates.add(criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")), likeKeyword));
                        break;
                }
            }

            // 역할 필터
            if (role != null) {
                predicates.add(criteriaBuilder.equal(root.get("role"), role));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


}