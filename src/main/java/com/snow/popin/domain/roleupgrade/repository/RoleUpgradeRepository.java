package com.snow.popin.domain.roleupgrade.repository;

import com.snow.popin.domain.roleupgrade.entity.ApprovalStatus;
import com.snow.popin.domain.roleupgrade.entity.RoleUpgrade;
import com.snow.popin.domain.user.constant.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoleUpgradeRepository extends JpaRepository<RoleUpgrade, Long> {

    // 특정 이메일의 승인 요청 목록
    List<RoleUpgrade> findByEmailOrderByCreatedAtDesc(String email);

    // 특정 이메일의 특정 역할 대기중 요청
    Optional<RoleUpgrade> findByEmailAndRequestedRoleAndStatus(String email, Role requestedRole, ApprovalStatus status);

    // 특정 이메일의 대기중 요청 존재 여부
    boolean existsByEmailAndStatus(String email, ApprovalStatus status);

    // 전체/상태/역할 페이징
    Page<RoleUpgrade> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<RoleUpgrade> findByStatusOrderByCreatedAtDesc(ApprovalStatus status, Pageable pageable);
    Page<RoleUpgrade> findByRequestedRoleOrderByCreatedAtDesc(Role requestedRole, Pageable pageable);
    Page<RoleUpgrade> findByStatusAndRequestedRoleOrderByCreatedAtDesc(ApprovalStatus status, Role requestedRole, Pageable pageable);

    // 대기중 개수
    long countByStatus(ApprovalStatus status);
}
