package com.snow.popin.domain.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snow.popin.domain.mypage.host.entity.Brand;
import com.snow.popin.domain.mypage.host.entity.Host;
import com.snow.popin.domain.mypage.host.entity.HostRole;
import com.snow.popin.domain.mypage.host.repository.BrandRepository;
import com.snow.popin.domain.mypage.host.repository.HostRepository;
import com.snow.popin.domain.mypage.provider.entity.ProviderProfile;
import com.snow.popin.domain.mypage.provider.repository.ProviderProfileRepository;
import com.snow.popin.domain.roleupgrade.dto.AdminUpdateRequest;
import com.snow.popin.domain.roleupgrade.dto.RoleUpgradeResponse;
import com.snow.popin.domain.roleupgrade.entity.ApprovalStatus;
import com.snow.popin.domain.roleupgrade.entity.RoleUpgrade;
import com.snow.popin.domain.roleupgrade.repository.RoleUpgradeRepository;
import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.domain.user.entity.User;
import com.snow.popin.domain.user.repository.UserRepository;
import com.snow.popin.global.constant.ErrorCode;
import com.snow.popin.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.snow.popin.domain.roleupgrade.service.RoleUpgradeService.validatePendingStatus;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdminRoleUpgradeService {

    private final RoleUpgradeRepository roleRepo;
    private final UserRepository userRepo;

    private final BrandRepository brandRepository;
    private final HostRepository hostRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final ObjectMapper objectMapper;

    // 관리자용: 모든 역할 승격 요청 페이징 조회
    public Page<RoleUpgradeResponse> getAllRoleUpgradeRequests(Pageable pageable){
        Page<RoleUpgrade> reqs = roleRepo.findAllByOrderByCreatedAtDesc(pageable);
        return reqs.map(RoleUpgradeResponse::from);
    }

    // 관리자용: 상태별 역할 승격 요청 조회
    public Page<RoleUpgradeResponse> getRoleUpgradeRequestsByStatus(ApprovalStatus status, Pageable pageable){
        Page<RoleUpgrade> reqs = roleRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
        return reqs.map(RoleUpgradeResponse::from);
    }

    // 관리자용: 요청 역할별 역할 승격 요청 조회
    public Page<RoleUpgradeResponse> getRoleUpgradeRequestsByRole(Role role, Pageable pageable){
        Page<RoleUpgrade> reqs = roleRepo.findByRequestedRoleOrderByCreatedAtDesc(role, pageable);
        return reqs.map(RoleUpgradeResponse::from);
    }

    // 관리자용: 상태와 역할 모두로 필터링한 역할 승격 요청 조회
    public Page<RoleUpgradeResponse> getRoleUpgradeRequestsByStatusAndRole(
            ApprovalStatus status, Role role,Pageable pageable ){
        Page<RoleUpgrade> reqs = roleRepo.findByStatusAndRequestedRoleOrderByCreatedAtDesc(status, role, pageable);
        return reqs.map(RoleUpgradeResponse::from);
    }

    // 관리자용: 역할 승격 요청 상세 조회 (권한 체크 없음)
    public RoleUpgradeResponse getRoleUpgradeRequestForAdmin(Long id) {
        RoleUpgrade roleUpgrade = roleRepo.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.ROLE_UPGRADE_REQUEST_NOT_FOUND));

        return RoleUpgradeResponse.from(roleUpgrade);
    }

    // 관리자용: 역할 승격 요청 처리 (승인/반려)
    @Transactional
    public void processRoleUpgradeRequest(Long id, AdminUpdateRequest req){
        RoleUpgrade roleUpgrade = roleRepo.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorCode.ROLE_UPGRADE_REQUEST_NOT_FOUND));

        validatePendingStatus(roleUpgrade);

        if (req.isApprove()){
            roleUpgrade.approve();
            User user = userRepo.findByEmail(roleUpgrade.getEmail())
                    .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

            user.updateRole(roleUpgrade.getRequestedRole());

            // 역할별 추가 데이터 생성
            createRoleSpecificData(user, roleUpgrade);

            log.info("역할 승격이 승인되었습니다. ID: {}, Email: {}, New Role: {}",
                    id, roleUpgrade.getEmail(), roleUpgrade.getRequestedRole());
        } else {
            roleUpgrade.reject(req.getRejectReason());
            log.info("역할 승격이 반려되었습니다. ID: {}, Reason: {}", id, req.getRejectReason());
        }
    }

    private void createRoleSpecificData(User user, RoleUpgrade roleUpgrade) {
        if (roleUpgrade.getRequestedRole() == Role.HOST) {
            createHostData(user, roleUpgrade.getPayload());
        } else if (roleUpgrade.getRequestedRole() == Role.PROVIDER) {
            createProviderData(user, roleUpgrade.getPayload());
        }
    }

    private void createHostData(User user, String payloadJson) {
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);

            Brand brand = Brand.builder()
                    .name(payload.get("brandName").asText())
                    .description(payload.has("brandDescription") ? payload.get("brandDescription").asText() : null)
                    .businessType(Brand.BusinessType.valueOf(payload.get("businessType").asText()))
                    .categoryId(payload.has("categoryId") ? payload.get("categoryId").asLong() : null)
                    .build();
            brandRepository.save(brand);

            Host host = Host.builder()
                    .brand(brand)
                    .user(user)
                    .roleInBrand(HostRole.OWNER)
                    .build();
            hostRepository.save(host);

        } catch (Exception e) {
            log.error("Host 데이터 생성 실패: {}", e.getMessage());
            throw new GeneralException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private void createProviderData(User user, String payloadJson) {
        try {
            JsonNode payload = objectMapper.readTree(payloadJson);

            ProviderProfile profile = new ProviderProfile();
            profile.setUserEmail(user.getEmail());
            profile.setName(payload.get("name").asText());
            profile.setPhone(payload.has("phone") ? payload.get("phone").asText() : null);
            profile.setBusinessRegistrationNumber(
                    payload.has("businessRegistrationNumber") ?
                            payload.get("businessRegistrationNumber").asText() : null
            );
            profile.setVerified(false);

            providerProfileRepository.save(profile);

        } catch (Exception e) {
            log.error("Provider 데이터 생성 실패: {}", e.getMessage());
            throw new GeneralException(ErrorCode.INTERNAL_ERROR);
        }
    }

    // 대기중인 승격 요청 개수 조회
    public Long getPendingRequestCount() {
        return roleRepo.countByStatus(ApprovalStatus.PENDING);
    }

    // 전체 승격 요청 개수 조회
    public Long getTotalRequestCount() {
        return roleRepo.count();
    }

    // 승인된 승격 요청 개수 조회
    public Long getApprovedRequestCount() {
        return roleRepo.countByStatus(ApprovalStatus.APPROVED);
    }

    // 거절된 승격 요청 개수 조회
    public Long getRejectedRequestCount() {
        return roleRepo.countByStatus(ApprovalStatus.REJECTED);
    }
}