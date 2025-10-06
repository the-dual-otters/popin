package com.snow.popin.domain.inquiry.entity;

import com.snow.popin.global.common.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status;

    @Builder
    public Inquiry(String email, TargetType targetType, Long targetId,
                   String subject, String content,InquiryStatus status) {
        this.email = email;
        this.targetType = targetType;
        this.targetId = targetId;
        this.subject = subject;
        this.content = content;
        this.status = status;
    }

    public void updateStatus(InquiryStatus status) {
        this.status = status;
    }

}
