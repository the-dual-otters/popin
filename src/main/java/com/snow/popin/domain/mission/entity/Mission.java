package com.snow.popin.domain.mission.entity;

import com.snow.popin.global.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission extends BaseEntity {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "BINARY(16)")
    @Type(type = "org.hibernate.type.UUIDBinaryType")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_set_id", nullable = false)
    private MissionSet missionSet;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 1000)
    private String description;

    // 정답(단순 비교용) — consider storing salted hash instead
    @Column(name = "answer", length = 255)
    private String answer;

    // 생성자
    @Builder
    public Mission(String title, String description, String answer, MissionSet missionSet) {
        this.title = title;
        this.description = description;
        this.answer = answer;
        this.missionSet = missionSet;
    }

    public void setMissionSet(MissionSet missionSet) {
        this.missionSet = missionSet;
    }
}
