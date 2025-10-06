package com.snow.popin.domain.roleupgrade.entity;

import com.snow.popin.domain.user.constant.Role;
import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "role_upgrades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleUpgrade extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email; // 신청자 이메일 (JWT에서 추출)

    @Column(name = "requested_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role requestedRole; // 요청하는 역할 (PROVIDER/HOST)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus status = ApprovalStatus.PENDING; // 승인 상태, default : 승인 대기

    @Column(name = "reject_reason")
    private String rejectReason; // 반려 사유

    @Column(columnDefinition = "JSON")
    private String payload; // 회사명, 사업자등록번호, 추가 작성사항 등을 JSON으로 저장

    @OneToMany(mappedBy = "roleUpgrade", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoleUpgradeDocument> documents = new ArrayList<>();

    @Builder
    public RoleUpgrade(String email, Role requestedRole, String payload) {
        this.email = email;
        this.requestedRole = requestedRole;
        this.payload = payload;
    }

    // 비즈니스 로직 메소드
    public void approve(){
        this.status = ApprovalStatus.APPROVED;
        this.rejectReason = null;
    }

    public void reject(String rejectReason){
        this.status = ApprovalStatus.REJECTED;
        this.rejectReason = rejectReason;
    }

    public void addDocument(RoleUpgradeDocument document){
        this.documents.add(document);
        document.setRoleUpgrade(this);
    }

    // 상태 체크 메소드들
    public boolean isPending() {
        return this.status == ApprovalStatus.PENDING;
    }

    public boolean isApproved() {
        return this.status == ApprovalStatus.APPROVED;
    }

    public boolean isRejected() {
        return this.status == ApprovalStatus.REJECTED;
    }

}
