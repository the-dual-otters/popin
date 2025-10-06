package com.snow.popin.domain.roleupgrade.entity;

import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name ="role_upgrade_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleUpgradeDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_upgrade_id", nullable = false)
    private RoleUpgrade roleUpgrade;

    @Column(name = "doc_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType docType;

    @Column(name = "business_number")
    private String businessNumber; // 사업자등록번호

    @Column(name = "file_url", nullable = false)
    private String fileUrl; // 파일 저장 경로

    @Builder
    public RoleUpgradeDocument(RoleUpgrade roleUpgrade, DocumentType docType,
                               String businessNumber, String fileUrl) {
        this.roleUpgrade = roleUpgrade;
        this.docType = docType;
        this.businessNumber = businessNumber;
        this.fileUrl = fileUrl;
    }

    // 연관관계 편의 메소드
    protected void setRoleUpgrade(RoleUpgrade roleUpgrade) {
        this.roleUpgrade = roleUpgrade;
    }


}
