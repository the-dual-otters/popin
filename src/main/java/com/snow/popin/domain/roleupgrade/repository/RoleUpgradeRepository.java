package com.snow.popin.domain.roleupgrade.repository;

import com.snow.popin.domain.roleupgrade.entity.ApprovalStatus;
import com.snow.popin.domain.roleupgrade.entity.RoleUpgrade;
import com.snow.popin.domain.user.constant.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleUpgradeRepository extends JpaRepository<RoleUpgrade, Long> {

    // 특정 이메일의 승인 요청 목록 조회
    List<RoleUpgrade> findByEmailOrderByCreatedAtDesc(String email);

    // 특정 이메일의 특정 역할에 대한 대기중인 요청 확인
    Optional<RoleUpgrade> findByEmailAndRequestedRoleAndStatus(String email, Role requestedRole, ApprovalStatus status);

    // 특정 이메일의 대기중인 요청이 있는지 확인
    boolean existsByEmailAndStatus(String email, ApprovalStatus status);

    // 관리자용 : 모든 승인 요청 페이징 조회
    Page<RoleUpgrade> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 관리자용: 상태별 승인 요청 페이징 조회
    Page<RoleUpgrade> findByStatusOrderByCreatedAtDesc(ApprovalStatus status, Pageable pageable);

    // 관리자용: 요청 역할별 승인 요청 페이징 조회
    Page<RoleUpgrade> findByRequestedRoleOrderByCreatedAtDesc(Role requestedRole, Pageable pageable);

    // 관리자용: 상태와 역할 모두로 필터링
    Page<RoleUpgrade> findByStatusAndRequestedRoleOrderByCreatedAtDesc(
            ApprovalStatus status, Role requestedRole, Pageable pageable);

    // 특정 이메일의 최신 요청 조회
    Optional<RoleUpgrade> findFirstByEmailOrderByCreatedAtDesc(String email);

    // 대기중인 요청 개수 조회 (관리자용)
    long countByStatus(ApprovalStatus status);

    // 특정 기간의 승인 요청 조회 (통계용)
    @Query("SELECT r FROM RoleUpgrade r WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate")
    List<RoleUpgrade> findByCreatedAtBetween(@Param("startDate") java.time.LocalDateTime startDate,
                                             @Param("endDate") java.time.LocalDateTime endDate);
}
