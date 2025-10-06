package com.snow.popin.domain.mission.entity;

import com.snow.popin.domain.mission.constant.MissionSetStatus;
import com.snow.popin.domain.popup.entity.Popup;
import com.snow.popin.domain.reward.entity.RewardOption;
import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionSet extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @Type(type = "org.hibernate.type.UUIDBinaryType")
    private UUID id;

    @Column(name = "popup_id", nullable = false)
    private Long popupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_id", insertable = false, updatable = false)
    private Popup popup;

    @Column(name = "required_count")
    private Integer requiredCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionSetStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "missionSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mission> missions = new ArrayList<>();

    @Column(name = "reward_pin", length = 80)
    private String rewardPin;

    @Column(name = "qr_image_url", length = 255)
    private String qrImageUrl;

    @OneToMany(mappedBy = "missionSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RewardOption> rewards = new ArrayList<>();



    @Builder
    public MissionSet(Long popupId, Integer requiredCount, MissionSetStatus status, String rewardPin) {
        this.popupId = popupId;
        this.requiredCount = requiredCount;
        this.status = (status != null) ? status : MissionSetStatus.ENABLED; // 기본값 활성화
        this.rewardPin = rewardPin;
    }

    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = MissionSetStatus.ENABLED;
        }
    }

    /**
     *비즈니스 메서드
     */
    public void addMission(Mission mission) {
        this.missions.add(mission);
        mission.setMissionSet(this);
    }

    public void addReward(RewardOption reward) {
        rewards.add(reward);
        reward.setMissionSet(this);
    }

    /**
     * 상태 전환
     */
    public void disable() {
        this.status = MissionSetStatus.DISABLED;
        this.completedAt = LocalDateTime.now();
    }

    public void enable() {
        this.status = MissionSetStatus.ENABLED;
        this.completedAt = null;
    }

    public boolean isEnabled() {
        return this.status == MissionSetStatus.ENABLED;
    }

    public boolean isDisabled() {
        return this.status == MissionSetStatus.DISABLED;
    }

    public boolean isCleared(long successCount) {
        int required = this.requiredCount != null ? this.requiredCount : 0;
        return successCount >= required;
    }

    public void setRequiredCount(Integer requiredCount) {
        this.requiredCount = requiredCount;
    }

    public void setStatus(MissionSetStatus status) {
        this.status = status;
    }

    public void setRewardPin(String rewardPin) {
        this.rewardPin = rewardPin;
    }

    public void setQrImageUrl(String qrImageUrl) {
        this.qrImageUrl = qrImageUrl;
    }
}
